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

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** represents module (or application module) properties */
public class ModuleProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleProperties.class);

    public static final String MODULE_PROPERTIES = "META-INF/lavender.properties";
    private static final String APP_PROPERTIES = "WEB-INF/lavender.properties";
    public static final List<String> DEFAULT_INCLUDES = new ArrayList<>(Arrays.asList(
            "**/*.gif", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.ico", "**/*.swf", "**/*.css", "**/*.js"));

    public static final Filter defaultFilter() {
        return new Filter().include(DEFAULT_INCLUDES);
    }

    public static ModuleProperties loadNode(boolean prod, Node root, Node pominfo) throws IOException {
        return ModuleProperties.parse(prod, root.readProperties(), pominfoOpt(pominfo));
    }

    public static ModuleProperties loadModuleOpt(boolean prod, Node root) throws IOException {
        Node src;
        Properties properties;

        if (root == null) {
            return null;
        }
        src = root.join(ModuleProperties.MODULE_PROPERTIES);
        if (!src.exists()) {
            return null;
        }
        properties = src.readProperties();
        return ModuleProperties.parse(prod, properties, pominfoOpt(root));
    }

    public static ModuleProperties loadApp(boolean prod, Node webapp) throws IOException {
        Node src;
        Properties pominfo;

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
        return parse(prod, src.readProperties(), pominfo);
    }

    private static Properties pominfoOpt(Node root) throws IOException {
        try {
            return root.join("META-INF/pominfo.properties").readProperties();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    // public only for testing
    public static ModuleProperties parse(boolean prod, Properties properties, Properties pominfo) throws IOException {
        ModuleProperties result;
        String relative;
        String source;
        String scmurlProd;
        String scmurlDevel;
        String tag;
        String svnsrc;

        relative = eat(properties, "pustefix.relative");
        // TODO: enforce pominfo != null when enough modules have switched
        if (!prod && pominfo != null && thisMachine(pominfo.getProperty("ethernet"))) {
            source = join(pominfo.getProperty("basedir"), relative);
        } else {
            source = null;
        }
        result = new ModuleProperties(eatFilter(properties, "pustefix", DEFAULT_INCLUDES), source);
        for (String prefix : prefixes(properties, ScmProperties.SVN_PREFIX)) {
            scmurlProd = (String) properties.remove(prefix);
            scmurlDevel = eatOpt(properties, prefix + ".devel", scmurlProd);
            tag = eatOpt(properties, prefix + ".revision", "-1");
            svnsrc = eatSvnSource(properties, prefix, source);
            svnsrc = fallback(scmurlProd, svnsrc);
            result.configs.add(
                    new ScmProperties(
                            prefix.substring(ScmProperties.SVN_PREFIX.length()),
                            eatFilter(properties, prefix, DEFAULT_INCLUDES),
                            scmurlProd, scmurlDevel, tag,
                            eatOpt(properties, prefix + ".type", Module.TYPE),
                            eatBoolean(properties, prefix + ".lavendelize", true),
                            eatOpt(properties, prefix + ".resourcePathPrefix", ""),
                            eatOpt(properties, prefix + ".targetPathPrefix", ""),
                            svnsrc));
        }
        for (String prefix : prefixes(properties, ScmProperties.SCM_PREFIX)) {
            scmurlProd = (String) properties.remove(prefix);
            scmurlDevel = eatOpt(properties, prefix + ".devel", scmurlProd);
            tag = eatOpt(properties, prefix + ".tag", "");
            String path = eatOpt(properties, prefix + ".path", "");
            if (!path.isEmpty() && !path.startsWith("/")) {
                path = "/" + path;
            }
            scmurlProd = scmurlProd + path;
            scmurlDevel = scmurlDevel + path;
            svnsrc = eatSvnSource(properties, prefix, source);
            svnsrc = fallback(scmurlProd, svnsrc);
            result.configs.add(
                    new ScmProperties(
                            prefix.substring(ScmProperties.SVN_PREFIX.length()),
                            eatFilter(properties, prefix, DEFAULT_INCLUDES),
                            scmurlProd, scmurlDevel, tag,
                            eatOpt(properties, prefix + ".type", Module.TYPE),
                            eatBoolean(properties, prefix + ".lavendelize", true),
                            eatOpt(properties, prefix + ".resourcePathPrefix", ""),
                            eatOpt(properties, prefix + ".targetPathPrefix", ""),
                            svnsrc));
        }
        if (properties.size() > 0) {
            throw new IllegalArgumentException("unknown properties: " + properties);
        }
        return result;
    }

    private static String eatSvnSource(Properties properties, String prefix, String source) {
        String relative;

        relative = eatOpt(properties, prefix + ".relative", null);
        return source == null || relative == null ? null : join(source, relative);
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

    private static boolean eatBoolean(Properties p, String key, boolean dflt) {
        String result;

        result = eatOpt(p, key, null);
        if (result == null) {
            return dflt;
        }
        switch (result) {
            case "true": return true;
            case "false": return false;
            default: throw new IllegalArgumentException("true or false expected, got " + result);
        }
    }

    private static String eat(Properties p, String key) {
        String result;

        result = eatOpt(p, key, null);
        if (result == null) {
            throw new IllegalArgumentException("key not found: " + key);
        }
        return result;
    }

    private static String eatOpt(Properties p, String key, String dflt) {
        String result;

        result = (String) p.remove(key);
        return result == null ? dflt : result;
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

    public final Filter filter;
    public final String source;
    public final Collection<ScmProperties> configs;

    public ModuleProperties(Filter filter, String source) {
        if (filter == null) {
            throw new IllegalArgumentException();
        }
        this.filter = filter;
        this.source = source;
        this.configs = new ArrayList<>();
    }

    public void addModules(FileNode cache, boolean prod, String svnUsername, String svnPassword, List<Module> result, JarConfig jarConfig)
            throws IOException {
        for (ScmProperties config : configs) {
            result.add(config.create(cache, prod, svnUsername, svnPassword, jarConfig));
        }
    }

    public Node live(Node root) {
        return source != null ? root.getWorld().file(source) : root;
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

    public static String toHex(byte ... bytes) {
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

    public static final Map<String, String> FALLBACK_SOURCES;

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

    //--

    /** To properly make jars available as a module, I have to load them into memory when the jar is itself packaged into a war. */
    public static Node[] loadStreamNodes(Node jar, String ... names) throws IOException {
        World world;
        int count;
        Node[] result;
        ZipEntry entry;
        String path;
        Node dest;
        int idx;

        world = jar.getWorld();
        count = 0;
        result = new Node[names.length];
        try (ZipInputStream src = new ZipInputStream(jar.newInputStream())) {
            while ((entry = src.getNextEntry()) != null) {
                path = entry.getName();
                idx = indexOf(names, path);
                if (idx != -1) {
                    count++;
                    dest = world.memoryNode();
                    result[idx] = dest;
                    world.getBuffer().copy(src, dest);
                    if (count == names.length) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    private static int indexOf(String[] all, String element) {
        for (int i = 0; i < all.length; i++) {
            if (element.equals(all[i])) {
                return i;
            }
        }
        return -1;
    }
}
