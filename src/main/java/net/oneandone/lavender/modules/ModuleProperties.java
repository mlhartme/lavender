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
import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Represents application properties or module properties which are both load from `lavender.properties` files. Factory for modules */
public class ModuleProperties extends PropertiesBase {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleProperties.class);

    public static final String MODULE_PROPERTIES = "META-INF/lavender.properties";
    private static final String APP_PROPERTIES = "WEB-INF/lavender.properties";
    private static final List<String> DEFAULT_INCLUDES = new ArrayList<>(Arrays.asList(
            "**/*.gif", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.ico", "**/*.swf", "**/*.css", "**/*.js"));

    //--

    public static ModuleProperties loadApp(boolean prod, Node webapp) throws IOException {
        Node src;
        Properties properties;

        src = webapp.join(ModuleProperties.APP_PROPERTIES);
        src.checkFile();
        properties = src.readProperties();
        return parse(prod, properties, pominfo(webapp.join("WEB-INF/classes")));
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
        return ModuleProperties.parse(prod, src.readProperties(), pominfo(root));
    }

    private static Properties pominfo(Node root) throws IOException {
        return root.join(PustefixJar.POMINFO_PROPERTIES).readProperties();
    }

    // public only for testing
    public static ModuleProperties parse(boolean prod, Properties properties, Properties pominfo) throws IOException {
        ModuleProperties result;
        String source;

        if (pominfo == null) {
            throw new IllegalArgumentException(properties.toString());
        }
        if (!prod && thisMachine(pominfo.getProperty("ethernet"))) {
            source = pominfo.getProperty("basedir");
        } else {
            source = null;
        }

        result = properties.containsKey("module.name") ? loadModern(properties, source) : loadClassic(properties, source);
        if (properties.size() > 0) {
            throw new IllegalArgumentException("unknown properties: " + properties);
        }
        return result;
    }

    private static ModuleProperties loadModern(Properties properties, String source) {
        String name;
        String scmurl;
        String revision;
        String path;

        name = eat(properties, "module.name");
        scmurl = eat(properties, "module.scmurl");
        revision = eat(properties, "module.revision");
        path = eatOpt(properties, "module.path", "");
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }
        return new ModuleProperties(
                name, false,
                eatFilter(properties, "module", DEFAULT_INCLUDES),
                redirect(scmurl), revision, path,
                eatBoolean(properties, "module.lavenderize", true),
                eatOpt(properties, "module.resourcePathPrefix", ""),
                eatOpt(properties, "module.targetPathPrefix", ""),
                eatIndex(properties),
                source == null ? null : join(source, path));
    }

    private static ModuleProperties loadClassic(Properties properties, String source) throws IOException {
        List<String> prefixes;
        String prefix;
        String tag;
        String scmurlProd;
        String scmurlDevel;

        prefixes = prefixes(properties, SCM_PREFIX);
        if (prefixes.size() != 1) {
            throw new IOException("one prefix expected, got " + prefixes);
        }
        prefix = prefixes.get(0);
        scmurlProd = (String) properties.remove(prefix);
        scmurlDevel = eatOpt(properties, prefix + ".devel", scmurlProd);
        tag = eatOpt(properties, prefix + ".tag", "");
        String path = eatOpt(properties, prefix + ".path", "");
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }
        if (!scmurlProd.equals(scmurlDevel)) {
            throw new IOException("scm url mismatch between dev + prod: " + scmurlProd + " vs " + scmurlDevel);
        }
        return new ModuleProperties(
                        prefix.substring(prefix.indexOf('.') + 1), true,
                        eatFilter(properties, prefix, DEFAULT_INCLUDES),
                        redirect(scmurlProd), tag, path,
                        eatBoolean(properties, prefix + ".lavendelize", true),
                        eatOpt(properties, prefix + ".resourcePathPrefix", ""),
                        eatOpt(properties, prefix + ".targetPathPrefix", ""),
                        null, source == null ? null : join(source, path));
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

    private static final Map<String, String> FALLBACK_REDIRECTS;

    static {
        String str;
        int idx;
        String dest;

        FALLBACK_REDIRECTS = new HashMap<>();
        str = System.getenv("LAVENDER_REDIRECTS");
        if (str != null) {
            for (String entry : Separator.COMMA.split(str)) {
                idx = entry.indexOf('=');
                if (idx == -1) {
                    throw new IllegalStateException("illegal redirect entry: " + entry);
                }
                dest = entry.substring(idx + 1);
                FALLBACK_REDIRECTS.put(entry.substring(0, idx), dest);
            }
            LOG.info("redirects configured: " + FALLBACK_REDIRECTS);
        }
    }

    private static String redirect(String url) {
        String prefix;

        for (Map.Entry<String, String> entry : FALLBACK_REDIRECTS.entrySet()) {
            prefix = entry.getKey();
            if (url.startsWith(prefix)) {
                return entry.getValue() + url.substring(prefix.length());
            }
        }
        return url;
    }

    //--

    public static final String SCM_PREFIX = "scm.";

    public final String name;
    public final boolean classic;
    public final Filter filter;
    public final String scmurl;

    /** for svn: revision number; git: commit hash */
    public final String revision;
    public final String path;
    public final boolean lavenderize;
    public final String resourcePathPrefix;
    public final String targetPathPrefix;

    public final Map<String, String> indexOpt;

    /** Absolute path relative to local sources for this module, null if not available */
    public final String source;

    /// CHECKSTYLE:OFF
    public ModuleProperties(String name, boolean classic, Filter filter, String scmurl, String revision, String path,
                         boolean lavenderize, String resourcePathPrefix, String targetPathPrefix, Map<String, String> indexOpt,
                         String source) {
        /// CHECKSTYLE:ON
        if (scmurl == null) {
            throw new NullPointerException();
        }
        if (name.startsWith("/") || name.endsWith("/")) {
            throw new IllegalArgumentException(name);
        }
        this.name = name;
        this.classic = classic;
        this.filter = filter;
        this.scmurl = scmurl;
        this.revision = revision;
        this.path = path;
        this.lavenderize = lavenderize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.indexOpt = indexOpt;
        this.source = source;
    }

    /** @param jarConfigOpt null when creating the application module or a temporary module for the scm command */
    public Module create(FileNode cacheDir, boolean prod, Secrets secrets, PustefixJarConfig jarConfigOpt) throws IOException {
        World world;
        final FileNode checkout;
        String scm;
        long pinnedRevision;

        world = cacheDir.getWorld();
        if (source != null) {
            checkout = world.file(source);
            if (checkout.isDirectory()) {
                LOG.info(name + ": create source module: " + source);
                // I could also check if the svnurl noted in the artifact matches the svn url of checkout,
                // but that fails for frontend teams creating a branch without adjusting scm elements in the pom.

                return new NodeModule(checkout, name, this, lavenderize, resourcePathPrefix, targetPathPrefix, filter) {
                    @Override
                    protected Map<String, Node> loadEntries() throws Exception {
                        Filter f;
                        final Map<String, Node> result;

                        result = new HashMap<>();
                        f = checkout.getWorld().filter().predicate(Predicate.FILE).includeAll();
                        f.invoke(checkout, new Action() {
                            public void enter(Node node, boolean isLink) {
                            }

                            public void enterFailed(Node node, boolean isLink, IOException e) throws IOException {
                                throw e;
                            }

                            public void leave(Node node, boolean isLink) {
                            }

                            public void select(Node node, boolean isLink) {
                                String relative;

                                relative = node.getRelative(checkout);
                                if (filter.matches(relative)) {
                                    if (jarConfigOpt != null) {
                                        relative = jarConfigOpt.getPath(relative);
                                    }
                                    if (relative != null) {
                                        result.put(relative, node);
                                    }
                                }
                            }
                        });
                        return result;
                    }
                };
            }
            // fall-through
        }
        scm = Strings.removeLeft(scmurl, "scm:");
        if (indexOpt != null) {
            return createIndexedModule(world, scm, revision, accessPathPrefix(path), jarConfigOpt, secrets);
        } else {
            if (scm.startsWith("svn:")) {
                pinnedRevision = !prod || revision.isEmpty() ? -1 : Long.parseLong(revision);
                return createSvnModule(cacheDir, jarConfigOpt, world, scm + path, secrets, pinnedRevision);
            } else if (scm.startsWith("git:")) {
                return createBitbucketModule(world, scm, secrets, accessPathPrefix(path), jarConfigOpt);
            } else {
                throw new IllegalStateException("scm url not supported: " + scm);
            }
        }
    }

    private static String accessPathPrefix(String path) {
        if (path.isEmpty()) {
            return path;
        } else {
            return Strings.removeLeft(path, "/") + "/";
        }
    }

    public static String urlToFilename(String url) {
        url = url.replace(":", "-");
        // CAUTION: place all files directly in the configured cache directory - sub directories would cause permission problems
        url = url.replace("/", "_");
        url = Strings.removeLeftOpt(url, ".");
        return url;
    }

    private IndexedModule createIndexedModule(World world, String scm, String at, String accessPathPrefix,
                                              PustefixJarConfig configOpt, Secrets secrets) throws IOException {
        UrlPattern urlPattern;

        if (!scm.startsWith("git:")) {
            throw new UnsupportedOperationException("TODO " + scm);
        }
        urlPattern = UrlPattern.create(world, scm, at, secrets);
        return new IndexedModule(scm, name, this, lavenderize, resourcePathPrefix, targetPathPrefix, filter,
                accessPathPrefix, configOpt, indexOpt, urlPattern);
    }

    private SvnModule createSvnModule(FileNode cacheDir, PustefixJarConfig jarConfig, World world, String scm, Secrets secrets, long pinnedRevision) throws IOException {
        SvnNode root;
        FileNode cache;

        try {
            root = (SvnNode) world.node(secrets.withSecrets(URI.create(scm)));
            // make sure to get a proper error message - and to get it early
            root.checkDirectory();
            cache = cacheDir.join("svn", urlToFilename(scm) + ".idx");
            cache.getParent().mkdirsOpt();
            return new SvnModule(name, this, cache, root, pinnedRevision, lavenderize, resourcePathPrefix, targetPathPrefix, filter, jarConfig);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("error scanning svn module " + scm + ": " + e.getMessage(), e);
        }
    }

    private BitbucketModule createBitbucketModule(World world, String urlstr, Secrets secrets,
                                                  String accessPathPrefix, PustefixJarConfig config) throws IOException {
        URI uri;
        UsernamePassword up;
        String uriPath;
        String project;
        String repository;
        int idx;

        up = secrets.get(urlstr);
        uri = URI.create(urlstr);
        if (!uri.getScheme().equals("git")) {
            throw new IllegalArgumentException("git uri expected, got " + urlstr);
        }
        uri = URI.create(uri.getSchemeSpecificPart());
        uriPath = Strings.removeLeft(uri.getPath(), "/");
        idx = uriPath.indexOf('/');
        project = uriPath.substring(0, idx);
        repository = Strings.removeRight(uriPath.substring(idx + 1), ".git");
        return new BitbucketModule(Bitbucket.create(world, uri.getHost(), up),
                project, repository, revision.isEmpty() ? "master" : revision, accessPathPrefix, name, this, lavenderize, resourcePathPrefix,
                targetPathPrefix, filter, config);
    }

    public String toString() {
        return "name: " + name + "\n"
                + "classic: " + classic + "\n"
                + "revision: " + revision + "\n"
                + "scmurl: " + scmurl + "\n"
                + "path: " + path + "\n"
                + "include: " + Strings.toList(filter.getIncludes()) + "\n"
                + "exclude: " + Strings.toList(filter.getExcludes()) + "\n"
                + "lavenderize: " + lavenderize + "\n"
                + "resourcePathPrefix: " + resourcePathPrefix + "\n"
                + "targetPathPrefix: " + targetPathPrefix;
    }
}
