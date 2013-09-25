/**
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

import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Filter;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.zip.ZipNode;
import net.oneandone.sushi.xml.Selector;
import net.oneandone.sushi.xml.XmlException;
import org.pustefixframework.live.LiveResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Iterates static resources of the Pustefix application. Valid static resource path are defined in WEB-INF/project.xml.
 * Resources can be found in the WAR or in nested JARs.
 *
 * This class is *not* called PustefixModule, because that term is also used by pustefix for it's modules.
 */
public class WarModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger(Module.class);

    public static final List<String> DEFAULT_INCLUDE_EXTENSIONS = new ArrayList<>(Arrays.asList(
            "gif", "png", "jpg", "jpeg", "ico", "swf", "css", "js"));

    public static final String PROPERTIES = "WEB-INF/lavender.properties";

    public static List<Module> fromWebapp(boolean prod, Node webapp, String svnUsername, String svnPassword) throws IOException {
        Node webappSource;
        List<Module> result;
        Properties properties;
        WarModule root;
        JarModuleConfig jarConfig;

        LOG.trace("scanning " + webapp);
        properties = getPropertiesOpt(webapp);
        if (properties == null) {
            throw new IOException("lavender.properties not found");
        }
        result = new ArrayList<>();
        webappSource = prod ? webapp : live(webapp);
        root = fromXml(Filter.forProperties(properties, "pustefix", DEFAULT_INCLUDE_EXTENSIONS), webappSource);
        result.add(root);
        for (Node jar : webapp.find("WEB-INF/lib/*.jar")) {
            jarConfig = loadJarModuleConfig(root, jar);
            if (jarConfig != null) {
                result.addAll(jarModule(prod, root, jar, jarConfig, svnUsername, svnPassword));
            }
        }
        for (SvnModuleConfig config : SvnModuleConfig.parse(properties)) {
            LOG.info("adding svn module " + config.folder);
            result.add(config.create(webapp.getWorld(), svnUsername, svnPassword));
        }
        return result;
    }

    public static List<Module> jarModule(boolean prod, WarModule root, Node jarOrig, JarModuleConfig config,
                                         String svnUsername, String svnPassword) throws IOException {
        List<Module> result;
        Node jarLive;
        Properties properties;
        Module jarModule;
        Node propertiesNode;

        result = new ArrayList<>();
        if (jarOrig instanceof FileNode) {
            if (prod) {
                jarLive = jarOrig;
            } else {
                jarLive = live(jarOrig);
            }
            if (jarLive.isFile()) {
                jarLive = ((FileNode) jarLive).openJar();
                propertiesNode = jarLive.join(PROPERTIES);
            } else {
                propertiesNode = ((FileNode) jarOrig).openJar().join(PROPERTIES);
            }
            jarModule = new JarFileModule(root.getFilter(), Docroot.WEB, config, jarLive);
        } else {
            if (!prod) {
                throw new UnsupportedOperationException("live mechanism not supported for jar streams");
            }
            jarModule = new JarStreamModule(root.getFilter(), Docroot.WEB, config, jarOrig);
            // TODO
            propertiesNode = null;
        }
        result.add(jarModule);

        if (propertiesNode != null && propertiesNode.exists()) {
            properties = propertiesNode.readProperties();
            // TODO: reject unknown properties
            for (SvnModuleConfig svnConfig : SvnModuleConfig.parse(properties)) {
                result.add(svnConfig.create(propertiesNode.getWorld(), svnUsername, svnPassword));
            }
        }
        return result;
    }


    public static Node live(Node root) throws IOException {
        File resolved;

        try {
            if (root instanceof FileNode) {
                resolved = LiveResolver.getInstance().resolveLiveRoot(((FileNode) root).getAbsolute(), "/foo");
            } else if (root instanceof ZipNode) {
                resolved = LiveResolver.getInstance().resolveLiveRoot(((ZipNode) root).getRoot().getZip().getName(), "/foo");
            } else {
                throw new IllegalArgumentException(root.toString());
            }
        } catch (Exception e) {
            throw new IOException("cannot resolve life root", e);
        }
        return resolved == null ? root : root.getWorld().file(resolved);
    }

    //--

    private static JarModuleConfig loadJarModuleConfig(WarModule parent, Node jar) throws IOException {
        ZipEntry entry;

        // TODO: expensive
        try (ZipInputStream jarInputStream = new ZipInputStream(jar.createInputStream())) {
            while ((entry = jarInputStream.getNextEntry()) != null) {
                if (entry.getName().equals("META-INF/pustefix-module.xml")) {
                    try {
                        return JarModuleConfig.load(jar.getWorld().getXml(), parent, jarInputStream);
                    } catch (SAXException | XmlException e) {
                        throw new IOException(jar + ": cannot load module descriptor:" + e.getMessage(), e);
                    }
                }
            }
        }
        return null;
    }

    public static Properties getPropertiesOpt(Node webapp) throws IOException {
        Node src;

        src = webapp.join(PROPERTIES);
        if (!src.exists()) {
            // TODO: dump this compatibility check as soon as I have ITs with new wars
            src = webapp.join("WEB-INF/lavendel.properties");
            if (!src.exists()) {
                return null;
            }
        }
        return src.readProperties();
    }

    public static WarModule fromXml(Filter filter, Node webapp) throws IOException {
        String path;
        Element root;
        Selector selector;
        String name;
        List<String> statics;

        try {
            root = webapp.join("WEB-INF/project.xml").readXml().getDocumentElement();
            selector = webapp.getWorld().getXml().getSelector();
            name = selector.string(root, "project/name");
            statics = new ArrayList<>();
            for (Element element : selector.elements(root, "application/static/path")) {
                path = element.getTextContent();
                path = path.trim();
                if (path.isEmpty() || path.startsWith("/")) {
                    throw new IllegalStateException(path);
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                statics.add(path);
            }
            return new WarModule(filter, name, statics, webapp);
        } catch (SAXException | XmlException e) {
            throw new IOException("cannot load project descriptor: " + e);
        }
    }

    //--

    private final Node webapp;
    private final List<String> statics;

    public WarModule(Filter filter, String name, List<String> statics, Node webapp) throws IOException {
        super(filter, Docroot.WEB, name, true, "");

        this.webapp = webapp;
        this.statics = statics;
    }

    public Iterator<Resource> iterator() {
        try {
            return WarResourceIterator.create(this, webapp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Resource probeIncluded(String path) throws IOException {
        Node node;

        if (!isPublicResource(path)) {
            return null;
        }
        node = webapp.join(path);
        if (!node.exists()) {
            return null;
        }
        return DefaultResource.forNode(node, path);
    }

    public boolean isPublicResource(String resourceName) {
        if (resourceName.startsWith("WEB-INF")) {
            return false;
        }

        for (String path : statics) {
            if (resourceName.startsWith(path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void saveCaches() {
        // nothing to do
    }
}
