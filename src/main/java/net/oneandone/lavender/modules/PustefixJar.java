package net.oneandone.lavender.modules;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A pustefix jar is a jar with pustefix module descriptor. It's not called pustefix module because it must not be confused with Lavender
 * modules; it can be possible to instantiate a Lavender module (more precisely: an embedded module, i.e. a module that loads all resources
 * from the underlying jar) from a pustefix jar.
 */
public abstract class PustefixJar {
    private static final String PUSTEFIX_MODULE_XML = "META-INF/pustefix-module.xml";
    private static final String RESOURCE_INDEX = "META-INF/pustefix-resource.index";
    public static final String POMINFO_PROPERTIES = "META-INF/pominfo.properties";

    /** @return null if not a pustefix jar */
    public static PustefixJar forNodeOpt(boolean prod, Node jar, WarConfig rootConfig) throws IOException {
        PustefixJar result;

        if (jar instanceof FileNode) {
            result = forFileNodeOpt(prod, (FileNode) jar, rootConfig);
        } else {
            if (!prod) {
                throw new UnsupportedOperationException("live mechanism not supported for jar streams");
            }
            result = forOtherNodeOpt(jar, rootConfig);
        }
        if (result != null) {
            if (result.moduleProperties != null && !result.hasResourceIndex) {
                throw new IOException("missing resource index: " + result.config.getModuleName());
            }
        }
        return result;
    }

    /** Loads resources from the jar into memory. */
    private static PustefixJar forOtherNodeOpt(Node jar, WarConfig warConfig) throws IOException {
        PustefixJarConfig config;
        ModuleProperties moduleProperties;
        boolean hasResourceIndex;

        Node[] loaded;
        Node propertyNode;

        loaded = loadStreamNodes(jar, PUSTEFIX_MODULE_XML, ModuleProperties.MODULE_PROPERTIES, POMINFO_PROPERTIES, RESOURCE_INDEX);
        if (loaded[0] == null) {
            return null;
        }
        config = PustefixJarConfig.load(loaded[0], warConfig);
        propertyNode = loaded[1];
        if (propertyNode == null) {
            moduleProperties = null;
        } else {
            if (loaded[2] == null) {
                throw new IOException("missing pominfo.properties in jar " + jar);
            }
            moduleProperties = ModuleProperties.loadModule(true, propertyNode, loaded[2]);
        }
        hasResourceIndex = loaded[3] != null;
        if (moduleProperties == null && hasResourceIndex) {
            // ok - we have a recent parent pom without lavender properties
            // -> the has not enabled lavender for this module
            return null;
        }
        return new PustefixJar(config, moduleProperties, hasResourceIndex) {
            @Override
            public Module createLegacyModule(Filter filter) throws IOException {
                World world;
                ZipEntry entry;
                String path;
                ZipInputStream src;
                Node root;
                Node child;
                Map<String, Node> files;
                String resourcePath;

                world = jar.getWorld();
                root = world.getMemoryFilesystem().root().node(UUID.randomUUID().toString(), null).mkdir();
                src = new ZipInputStream(jar.newInputStream());
                files = new HashMap<>();
                while ((entry = src.getNextEntry()) != null) {
                    path = entry.getName();
                    if (!entry.isDirectory()) {
                        if ((resourcePath = config.getPath(path)) != null && filter.matches(path)) {
                            child = root.join(path);
                            child.getParent().mkdirsOpt();
                            world.getBuffer().copy(src, child);
                            files.put(resourcePath, child);
                        }
                    }
                }

                return new NodeModule(jar.getName() /* this is the artifact name with the version */, Module.TYPE, config.getModuleName(), true, config.getResourcePathPrefix(), "", filter) {
                    public Map<String, Node> loadEntries() {
                        // no need to re-loadEntries files from memory
                        return files;
                    }
                };
            }
        };
    }

    private static PustefixJar forFileNodeOpt(boolean prod, FileNode jarOrig, WarConfig warConfig) throws IOException {
        PustefixJarConfig config;
        ModuleProperties moduleProperties;
        boolean hasResourceIndex;

        Node exploded;
        Node configFile;

        exploded = jarOrig.openJar();
        configFile = exploded.join(PUSTEFIX_MODULE_XML);
        if (!configFile.exists()) {
            return null;
        }
        config = PustefixJarConfig.load(configFile, warConfig);
        moduleProperties = ModuleProperties.loadModuleOpt(prod, exploded);
        if (moduleProperties == null) {
            return null;
        }
        hasResourceIndex = exploded.join(RESOURCE_INDEX).exists();
        return new PustefixJar(config, moduleProperties, hasResourceIndex) {
            @Override
            public Module createLegacyModule(Filter filter) throws IOException {
                Node jarLive;

                jarLive = jarOrig.isFile() ? jarOrig.openJar() : jarOrig;
                return new NodeModule(jarOrig.getName(), Module.TYPE, config.getModuleName(), true, config.getResourcePathPrefix(), "", filter) {
                    @Override
                    protected Map<String, Node> loadEntries() throws IOException {
                        return files(filter, config, jarLive);
                    }
                };
            }
        };
    }

    private static Map<String, Node> files(final Filter filter, final PustefixJarConfig config, final Node exploded) throws IOException {
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

    /** To properly make jars available as a module, I have to load them into memory when the jar is itself packaged into a war. */
    private static Node[] loadStreamNodes(Node jar, String ... names) throws IOException {
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
                        break;
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

    //--

    public final PustefixJarConfig config;
    public final ModuleProperties moduleProperties;
    public final boolean hasResourceIndex;

    public PustefixJar(PustefixJarConfig config, ModuleProperties moduleProperties, boolean hasResourceIndex) {
        this.config = config;
        this.moduleProperties = moduleProperties;
        this.hasResourceIndex = hasResourceIndex;
    }

    /**
     * @return an embedded module that serves all resources from the jar itself. Caching for this module is based on the jar file name,
     * i.e. the artifact and the version
     */
    public abstract Module createLegacyModule(Filter filter) throws IOException;
}
