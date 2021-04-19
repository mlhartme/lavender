/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.lavender.modules;

import net.oneandone.lavender.config.PropertiesBase;
import net.oneandone.lavender.config.Secrets;
import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Represents application properties or module properties. Factory for modules */
public class ModuleProperties extends PropertiesBase {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleProperties.class);

    public static final String MODULE_PROPERTIES = "META-INF/lavender.properties";
    private static final String APP_PROPERTIES = "WEB-INF/lavender.properties";
    private static final List<String> DEFAULT_INCLUDES = new ArrayList<>(Arrays.asList(
            "**/*.gif", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.ico", "**/*.swf", "**/*.css", "**/*.js"));

    public static Filter defaultFilter() {
        return new Filter().include(DEFAULT_INCLUDES);
    }

    //--

    public static ModuleProperties loadApp(boolean prod, Node webapp) throws IOException {
        Node src;
        Properties pominfo;
        Properties properties;

        src = webapp.join(ModuleProperties.APP_PROPERTIES);
        src.checkFile();
        pominfo = pominfoOpt(webapp.join("WEB-INF/classes"));
        if (pominfo == null) {
            // TODO: try target/classes - hack because pws deletes WEB-INF/classes to get virtual classpath working
            pominfo = pominfoOpt(webapp.join("../classes"));
        }
        if (pominfo == null) {
            throw new IOException("pominfo.properties for application not found");
        }
        properties = src.readProperties();
        return parse(prod, properties, pominfo);
    }

    public static ModuleProperties loadModule(boolean prod, Node root, Node pominfo) throws IOException {
        return ModuleProperties.parse(prod, root.readProperties(), pominfo.readProperties());
    }

    public static ModuleProperties loadModuleOpt(boolean prod, Node root) throws IOException {
        Node src;

        if (root == null) {
            return null;
        }
        src = root.join(ModuleProperties.MODULE_PROPERTIES);
        if (!src.exists()) {
            return null;
        }
        return ModuleProperties.parse(prod, src.readProperties(), pominfoOpt(root));
    }

    private static Properties pominfoOpt(Node root) throws IOException {
        try {
            return root.join(PustefixJar.POMINFO_PROPERTIES).readProperties();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    // public only for testing
    public static ModuleProperties parse(boolean prod, Properties properties, Properties pominfo) throws IOException {
        ModuleProperties result;
        String source;
        String scmurlProd;
        String scmurlDevel;
        String tag;
        String scmsrc;

        if (pominfo == null) {
            throw new IOException("pominfo.properties for module not found: " + properties);
        }
        if (eatOpt(properties, "pustefix.relative", null) != null) {
            throw new IOException("pustefix.relative is no longer supported: " + pominfo.toString());
        }
        if (!prod && thisMachine(pominfo.getProperty("ethernet"))) {
            source = pominfo.getProperty("basedir");
        } else {
            source = null;
        }

        result = new ModuleProperties();
        for (String prefix : prefixes(properties, ScmProperties.SCM_PREFIX)) {
            scmurlProd = (String) properties.remove(prefix);
            scmurlDevel = eatOpt(properties, prefix + ".devel", scmurlProd);
            tag = eatOpt(properties, prefix + ".tag", "");
            String path = eatOpt(properties, prefix + ".path", "");
            if (!path.isEmpty() && !path.startsWith("/")) {
                path = "/" + path;
            }
            scmsrc = fallback(scmurlProd, source == null ? null : join(source, path));
            result.configs.add(
                    new ScmProperties(
                            prefix.substring(prefix.indexOf('.') + 1),
                            eatFilter(properties, prefix, DEFAULT_INCLUDES),
                            scmurlProd, scmurlDevel, tag, path,
                            eatOpt(properties, prefix + ".type", Module.TYPE),
                            eatBoolean(properties, prefix + ".lavendelize", true),
                            eatOpt(properties, prefix + ".resourcePathPrefix", ""),
                            eatOpt(properties, prefix + ".targetPathPrefix", ""),
                            scmsrc));
        }
        if (properties.size() > 0) {
            throw new IllegalArgumentException("unknown properties: " + properties);
        }
        return result;
    }

    private static String join(String left, String right) {
        StringBuilder result;

        result = new StringBuilder(left);
        if (!left.endsWith("/")) {
            result.append("/");
        }
        if (right.startsWith("/")) {
            right = right.substring(1);
        }
        result.append(right);
        return result.toString();
    }

    private static Filter eatFilter(Properties properties, String prefix, List<String> defaultIncludes) {
        Filter result;

        result = new Filter();
        result.include(eatListOpt(properties, prefix + ".includes", defaultIncludes));
        result.exclude(eatListOpt(properties, prefix + ".excludes", Collections.EMPTY_LIST));
        return result;
    }

    private static List<String> eatListOpt(Properties p, String key, List<String> dflt) {
        String result;

        result = eatOpt(p, key, null);
        return result == null ? dflt : Separator.COMMA.split(result);
    }

    private static List<String> prefixes(Properties properties, String prefix) {
        List<String> result;

        result = new ArrayList<>();
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                if (Strings.count(name, ".") == 1) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    //--

    public final Collection<ScmProperties> configs;

    public ModuleProperties() {
        this.configs = new ArrayList<>();
    }

    public void createModules(FileNode cache, boolean prod, Secrets secrets, List<Module> result, PustefixJarConfig jarConfig)
            throws IOException {
        for (ScmProperties config : configs) {
            result.add(config.create(cache, prod, secrets, jarConfig));
        }
    }

    //--

    private static boolean thisMachine(String ethernet) throws IOException {
        List<String> thisEthernet;
        List<String> otherEthernet;

        thisEthernet = ethernet();
        otherEthernet = Separator.COMMA.split(ethernet);
        for (String address : thisEthernet) {
            if (otherEthernet.contains(address)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> ethernet() throws IOException {
        Enumeration ifcs;
        List<String> result;
        NetworkInterface ifc;
        byte[] address;

        ifcs = NetworkInterface.getNetworkInterfaces();
        result = new ArrayList<>();
        while (ifcs.hasMoreElements()) {
            ifc = (NetworkInterface) ifcs.nextElement();
            address = ifc.getHardwareAddress();
            if (address != null) {
                result.add(toHex(address));
            } else {
                // ignore -- not available (i.e. loopback device) or not accessible
            }
        }
        return result;
    }

    private static String toHex(byte... bytes) {
        StringBuilder builder;

        builder = new StringBuilder();
        for (byte b : bytes) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(toHexChar(b >> 4 & 0xf));
            builder.append(toHexChar(b & 0xf));
        }
        return builder.toString();
    }

    private static char toHexChar(int b) {
        if (b >= 0 && b <= 9) {
            return (char) ('0' + b);
        } else if (b >= 10 && b <= 15) {
            return (char) ('a' + b - 10);
        } else {
            throw new IllegalArgumentException("" + b);
        }
    }

    //--

    private static final Map<String, String> FALLBACK_SOURCES;

    static {
        String str;
        int idx;
        String file;

        FALLBACK_SOURCES = new HashMap<>();
        str = System.getenv("LAVENDER_FALLBACKS");
        if (str != null) {
            for (String entry : Separator.COMMA.split(str)) {
                idx = entry.indexOf('=');
                if (idx == -1) {
                    throw new IllegalStateException("illegal fallback entry: " + entry);
                }
                file = entry.substring(idx + 1);
                if (!new java.io.File(file).isDirectory()) {
                    throw new IllegalStateException("fallback directory not found: " + file);
                }
                FALLBACK_SOURCES.put(entry.substring(0, idx), file);
            }
            LOG.info("fallback configured: " + FALLBACK_SOURCES);
        }
    }

    private static String fallback(String url, String source) {
        String fallbackSource;

        fallbackSource = FALLBACK_SOURCES.get(url);
        if (!FALLBACK_SOURCES.isEmpty()) {
            LOG.info("fallback for url " + url + ": " + fallbackSource);
        }
        return fallbackSource != null ? fallbackSource : source;
    }
}
