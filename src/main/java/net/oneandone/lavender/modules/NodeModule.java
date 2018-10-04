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

import net.oneandone.lavender.config.Secrets;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.xml.Selector;
import net.oneandone.sushi.xml.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class NodeModule extends Module<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(Module.class);
    // used to detect a recent parent pom
    private static final String RESOURCE_INDEX = "META-INF/pustefix-resource.index";

    /** @param legacy returns the legacy modules configured or scanned */
    public static List<Module> fromWebapp(FileNode cache, boolean prod, Node<?> webapp, Secrets secrets, boolean scanLegacy, List<String> legacy)
            throws IOException, SAXException, XmlException {
        Node<?> webappSource;
        List<Module> result;
        WarConfig rootConfig;
        NodeModule root;
        ModuleProperties application;

        LOG.trace("scanning " + webapp);
        application = ModuleProperties.loadApp(prod, webapp, scanLegacy ? null : legacy);
        result = new ArrayList<>();
        rootConfig = WarConfig.fromXml(webapp);
        // add modules before webapp, because they have a prefix
        for (Node<?> jar : webapp.find("WEB-INF/lib/*.jar")) {
            result.addAll(jarModuleOpt(cache, rootConfig, prod, jar, secrets, scanLegacy, legacy));
        }
        webappSource = application.live(webapp);
        root = warModule(rootConfig, application.filter, webappSource);
        result.add(root);
        application.addModules(cache, prod, secrets, result, null);
        return result;
    }

    /** @param legacy null to detect legcy modules; in this case, result != indicates a legacy module */
    public static List<Module> jarModuleOpt(FileNode cache, WarConfig rootConfig, boolean prod, Node jarOrig, Secrets secrets, boolean checkLegacy, List<String> legacy)
            throws IOException, XmlException, SAXException {
        Info info;
        List<Module> result;

        result = new ArrayList<>();
        if (jarOrig instanceof FileNode) {
            info = Info.forFileNode(prod, (FileNode) jarOrig, rootConfig);
        } else {
            if (!prod) {
                throw new UnsupportedOperationException("live mechanism not supported for jar streams");
            }
            info = Info.forOtherNode(jarOrig, rootConfig);
        }
        if (info == null) {
            return result;
        }
        if (info.lp != null && !info.hasResourceIndex) {
            throw new IOException("missing resource index: " + jarOrig.getUri().toString());
        }
        if (info.lp == null && info.hasResourceIndex) {
            throw new IOException("missing lavender.properties: " + jarOrig.getUri().toString());
        }

        if (checkLegacy) {
            if (info.lp == null && !info.hasResourceIndex) {
                if (info.jarModule.iterator().hasNext()) {
                    legacy.add(info.jarModule.getName());
                }
            }
        } else {
            if (!legacy.contains(info.jarModule.getName())) {
                if (info.lp == null && !info.hasResourceIndex) {
                    if (info.jarModule.iterator().hasNext()) {
                        throw new IOException("missing lavender.properties: " + info.jarModule.getName());
                    } else {
                        // no entries
                    }
                }
            }
        }
        // continue without lavender.properties -- we have to support this mode for a some time ... :(
        result.add(info.jarModule);
        if (info.lp != null) {
            info.lp.addModules(cache, prod, secrets, result, info.config);
        }
        return result;
    }

    public static class Info {
        public static Info forOtherNode(Node jar, WarConfig rootConfig) throws IOException {
            Info info;
            Object[] tmp;

            info = new Info();
            tmp = NodeModule.fromJarStream(rootConfig, jar);
            if (tmp == null) {
                // no pustefix module config
                return null;
            }
            info.jarModule = (Module) tmp[0];
            info.lp = (ModuleProperties) tmp[1];
            info.config = (JarConfig) tmp[2];
            info.hasResourceIndex = (Boolean) tmp[3];
            if (info.lp == null && info.hasResourceIndex) {
                // ok - we have a recent parent pom without lavender properties
                // -> the has not enabled lavender for this module
                return null;
            }
            return info;
        }

        public static Info forFileNode(boolean prod, FileNode jarOrig, WarConfig rootConfig) throws IOException, XmlException, SAXException {
            Info info;
            Node exploded;
            Node configFile;
            Node jarTmp;
            Node jarLive;
            Filter filter;

            info = new Info();
            // TODO: expensive
            exploded = jarOrig.openJar();
            configFile = exploded.join("META-INF/pustefix-module.xml");
            if (!configFile.exists()) {
                return null;
            }
            try (InputStream src = configFile.newInputStream()) {
                info.config = JarConfig.load(jarOrig.getWorld().getXml(), rootConfig, src);
            }
            info.lp = ModuleProperties.loadModuleOpt(prod, exploded);
            if (info.lp == null) {
                return null;
            }
            info.hasResourceIndex = exploded.join(RESOURCE_INDEX).exists();
            jarTmp = prod ? jarOrig : info.lp.live(jarOrig);
            if (jarTmp.isFile()) {
                jarLive = ((FileNode) jarTmp).openJar();
            } else {
                jarLive = jarTmp;
            }
            filter = info.lp.filter;
            info.jarModule = new NodeModule(Module.TYPE, info.config.getModuleName(), true, info.config.getResourcePathPrefix(), "", filter) {
                @Override
                protected Map<String, Node> loadEntries() throws IOException {
                    return files(filter, info.config, jarLive);
                }
            };
            return info;
        }

        public JarConfig config;
        public ModuleProperties lp;
        public boolean hasResourceIndex;
        public Module jarModule;
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

    public static NodeModule warModule(final WarConfig config, final Filter filter, final Node webapp) throws IOException {
        Element root;
        Selector selector;
        String name;

        try {
            root = webapp.join("WEB-INF/project.xml").readXml().getDocumentElement();
            selector = webapp.getWorld().getXml().getSelector();
            name = selector.string(root, "project/name");
            return new NodeModule(Module.TYPE, name, true, "", "", filter) {
                @Override
                protected Map<String, Node> loadEntries() throws IOException {
                    return scanExploded(config, filter, webapp);
                }
            };
        } catch (SAXException | XmlException e) {
            throw new IOException("cannot load project descriptor: " + e);
        }
    }

    private static Map<String, Node> scanExploded(final WarConfig global, final Filter filter, final Node exploded) throws IOException {
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

                path = node.getRelative(exploded);
                if (filter.matches(path) && global.isPublicResource(path)) {
                    result.put(path, node);
                }
            }
        });
        return result;
    }

    //--

    /** To properly make jars available as a module, I have to load them into memory when the jar is itself contained in a war. */
    public static Object[] fromJarStream(WarConfig rootConfig, Node jar) throws IOException {
        JarConfig config;
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
        ModuleProperties lp;

        loaded = ModuleProperties.loadStreamNodes(jar, "META-INF/pustefix-module.xml",
                ModuleProperties.MODULE_PROPERTIES, "META-INF/pominfo.properties", RESOURCE_INDEX);
        if (loaded[0] == null) {
            return null;
        }
        try (InputStream configSrc = loaded[0].newInputStream()) {
            config = JarConfig.load(jar.getWorld().getXml(), rootConfig, configSrc);
        } catch (SAXException | XmlException e) {
            throw new IOException(jar + ": cannot load module descriptor:" + e.getMessage(), e);
        }
        propertyNode = loaded[1];
        if (propertyNode == null) {
            filter = ModuleProperties.defaultFilter();
            lp = null;
        } else {
            if (loaded[2] == null) {
                throw new IOException("missing pominfo.properties in jar " + jar);
            }
            lp = ModuleProperties.loadNode(true, propertyNode, loaded[2]);
            filter = lp.filter;
        }
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
        return new Object[] { new NodeModule(Module.TYPE, config.getModuleName(), true, config.getResourcePathPrefix(), "", filter) {
            public Map<String, Node> loadEntries() {
                // no need to re-loadEntries files from memory
                return files;
            }
        }, lp, config, loaded[3] != null};
    }

    //--

    public NodeModule(String type, String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        super(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
    }

    protected Resource createResource(String resourcePath, Node file) throws IOException {
        return NodeResource.forNode(file, resourcePath);
    }
}
