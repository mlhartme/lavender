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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class NodeModule extends Module<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(Module.class);
    // used to detect a recent parent pom

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
        Embedded embedded;
        List<Module> result;

        result = new ArrayList<>();
        if (jarOrig instanceof FileNode) {
            embedded = Embedded.forFileNode(prod, (FileNode) jarOrig, rootConfig);
        } else {
            if (!prod) {
                throw new UnsupportedOperationException("live mechanism not supported for jar streams");
            }
            embedded = Embedded.forOtherNode(jarOrig, rootConfig);
        }
        if (embedded == null) {
            return result;
        }
        if (embedded.lp != null && !embedded.hasResourceIndex) {
            throw new IOException("missing resource index: " + jarOrig.getUri().toString());
        }
        if (embedded.lp == null && embedded.hasResourceIndex) {
            throw new IOException("missing lavender.properties: " + jarOrig.getUri().toString());
        }

        if (checkLegacy) {
            if (embedded.lp == null && !embedded.hasResourceIndex) {
                if (embedded.jarModule.iterator().hasNext()) {
                    legacy.add(embedded.jarModule.getName());
                }
            }
        } else {
            if (!legacy.contains(embedded.jarModule.getName())) {
                if (embedded.lp == null && !embedded.hasResourceIndex) {
                    if (embedded.jarModule.iterator().hasNext()) {
                        throw new IOException("missing lavender.properties: " + embedded.jarModule.getName());
                    } else {
                        // no entries
                    }
                }
            }
        }
        // continue without lavender.properties -- we have to support this mode for a some time ... :(
        result.add(embedded.jarModule);
        if (embedded.lp != null) {
            embedded.lp.addModules(cache, prod, secrets, result, embedded.config);
        }
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

    public NodeModule(String type, String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        super(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
    }

    protected Resource createResource(String resourcePath, Node file) throws IOException {
        return NodeResource.forNode(file, resourcePath);
    }
}
