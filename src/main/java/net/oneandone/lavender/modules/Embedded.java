package net.oneandone.lavender.modules;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.xml.XmlException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** To create modules that load resources from jars. */
public class Embedded {
    private static final String RESOURCE_INDEX = "META-INF/pustefix-resource.index";

    public static Embedded forNodeOpt(boolean prod, Node jar, WarConfig rootConfig) throws IOException, SAXException, XmlException {
        if (jar instanceof FileNode) {
            return forFileNodeOpt(prod, (FileNode) jar, rootConfig);
        } else {
            if (!prod) {
                throw new UnsupportedOperationException("live mechanism not supported for jar streams");
            }
            return forOtherNodeOpt(jar, rootConfig);
        }
    }

    /** To properly make jars available as a module, I have to load them into memory when the jar is itself contained in a war. */
    public static Embedded forOtherNodeOpt(Node jar, WarConfig rootConfig) throws IOException {
        Embedded embedded;

        Node[] loaded;
        Filter filter;
        World world;
        ZipEntry entry;
        String path;
        ZipInputStream src;
        Node root;
        Node child;
        Node propertyNode;
        final Map<String, Node> files;
        String resourcePath;

        embedded = new Embedded();
        loaded = ModuleProperties.loadStreamNodes(jar, "META-INF/pustefix-module.xml",
                ModuleProperties.MODULE_PROPERTIES, "META-INF/pominfo.properties", RESOURCE_INDEX);
        if (loaded[0] == null) {
            return null;
        }
        try (InputStream configSrc = loaded[0].newInputStream()) {
            embedded.config = JarConfig.load(jar.getWorld().getXml(), rootConfig, configSrc);
        } catch (SAXException | XmlException e) {
            throw new IOException(jar + ": cannot load module descriptor:" + e.getMessage(), e);
        }
        propertyNode = loaded[1];
        if (propertyNode == null) {
            filter = ModuleProperties.defaultFilter();
            embedded.lp = null;
        } else {
            if (loaded[2] == null) {
                throw new IOException("missing pominfo.properties in jar " + jar);
            }
            embedded.lp = ModuleProperties.loadNode(true, propertyNode, loaded[2]);
            filter = embedded.lp.filter;
        }
        world = jar.getWorld();
        root = world.getMemoryFilesystem().root().node(UUID.randomUUID().toString(), null).mkdir();
        src = new ZipInputStream(jar.newInputStream());
        files = new HashMap<>();
        while ((entry = src.getNextEntry()) != null) {
            path = entry.getName();
            if (!entry.isDirectory()) {
                if ((resourcePath = embedded.config.getPath(path)) != null && filter.matches(path)) {
                    child = root.join(path);
                    child.getParent().mkdirsOpt();
                    world.getBuffer().copy(src, child);
                    files.put(resourcePath, child);
                }
            }
        }
        embedded.jarModule = new NodeModule(Module.TYPE, embedded.config.getModuleName(), true, embedded.config.getResourcePathPrefix(), "", filter) {
            public Map<String, Node> loadEntries() {
                // no need to re-loadEntries files from memory
                return files;
            }
        };
        embedded.hasResourceIndex = loaded[3] != null;
        if (embedded.lp == null && embedded.hasResourceIndex) {
            // ok - we have a recent parent pom without lavender properties
            // -> the has not enabled lavender for this module
            return null;
        }
        return embedded;
    }

    public static Embedded forFileNodeOpt(boolean prod, FileNode jarOrig, WarConfig rootConfig) throws IOException, XmlException, SAXException {
        Embedded embedded;
        Node exploded;
        Node configFile;
        Node jarTmp;
        Node jarLive;
        Filter filter;

        embedded = new Embedded();
        exploded = jarOrig.openJar();
        configFile = exploded.join("META-INF/pustefix-module.xml");
        if (!configFile.exists()) {
            return null;
        }
        try (InputStream src = configFile.newInputStream()) {
            embedded.config = JarConfig.load(jarOrig.getWorld().getXml(), rootConfig, src);
        }
        embedded.lp = ModuleProperties.loadModuleOpt(prod, exploded);
        if (embedded.lp == null) {
            return null;
        }
        embedded.hasResourceIndex = exploded.join(RESOURCE_INDEX).exists();
        jarTmp = prod ? jarOrig : embedded.lp.live(jarOrig);
        if (jarTmp.isFile()) {
            jarLive = ((FileNode) jarTmp).openJar();
        } else {
            jarLive = jarTmp;
        }
        filter = embedded.lp.filter;
        embedded.jarModule = new NodeModule(Module.TYPE, embedded.config.getModuleName(), true, embedded.config.getResourcePathPrefix(), "", filter) {
            @Override
            protected Map<String, Node> loadEntries() throws IOException {
                return files(filter, embedded.config, jarLive);
            }
        };
        return embedded;
    }

    private static Map<String, Node> files(final Filter filter, final JarConfig config, final Node exploded) throws IOException {
        Filter f;
        final Map<String, Node> result;

        result = new HashMap<>();
        f = exploded.getWorld().filter().predicate(Predicate.FILE).includeAll();
        f.invoke(exploded, new Action() {
            public void enter(Node node, boolean isLink) {
            }

            public void enterFailed(Node node, boolean isLink, IOException e) throws IOException {
                throw e;
            }

            public void leave(Node node, boolean isLink) {
            }

            public void select(Node node, boolean isLink) {
                String path;
                String resourcePath;

                path = node.getRelative(exploded);
                if (filter.matches(path)) {
                    resourcePath = config.getPath(path);
                    if (resourcePath != null) {
                        result.put(resourcePath, node);
                    }
                }
            }
        });
        return result;
    }

    //--

    public JarConfig config;
    public ModuleProperties lp;
    public boolean hasResourceIndex;
    public Module jarModule;
}
