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

import net.oneandone.lavender.config.Filter;
import net.oneandone.lavender.config.View;
import net.oneandone.lavender.index.Resource;
import net.oneandone.lavender.modules.project.ProjectConfig;
import net.oneandone.sushi.fs.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Iterates static resources from a Pustefix application. Valid static resource path are defined in WEB-INF/project.xml.
 * Resources can be found in the WAR or in nested JARs.
 */
public class PustefixModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger(Module.class);

    private static final List<String> DEFAULT_INCLUDE_EXTENSIONS = new ArrayList<>(Arrays.asList(
            "gif", "png", "jpg", "jpeg", "ico", "swf", "css", "js"));

    private static final String PROPERTIES = "WEB-INF/lavender.properties";

    public static List<Module> fromWebapp(Node webapp, String svnUsername, String svnPassword) throws IOException {
        List<Module> result;
        Properties properties;
        PustefixModule ps;
        PustefixModuleConfig mc;

        LOG.trace("scanning " + webapp);
        result = new ArrayList<>();
        properties = getProperties(webapp);
        ps = create(Filter.forProperties(properties, "pustefix", DEFAULT_INCLUDE_EXTENSIONS), webapp);
        result.add(ps);
        for (Node jar : webapp.find("WEB-INF/lib/*.jar")) {
            mc = loadModuleXml(ps, jar);
            if (mc != null) {
                result.add(new JarModule(ps.getFilter(), View.WEB, mc, jar));
            }
        }
        for (SvnModuleConfig config : SvnModuleConfig.parse(properties)) {
            LOG.info("adding svn module " + config.folder);
            result.add(config.create(webapp.getWorld(), svnUsername, svnPassword));
        }
        return result;
    }

    private static PustefixModuleConfig loadModuleXml(PustefixModule module, Node jar) throws IOException {
        ZipInputStream jarInputStream;
        ZipEntry jarEntry;

        jarInputStream = new ZipInputStream(jar.createInputStream());
        while ((jarEntry = jarInputStream.getNextEntry()) != null) {
            if (isModuleXml(jarEntry)) {
                try {
                    return new PustefixModuleConfig(module, jarInputStream);
                } catch (JAXBException e) {
                    throw new IOException("cannot load module descriptor", e);
                }
            }
        }
        return null;
    }

    private static boolean isModuleXml(ZipEntry entry) {
        return entry.getName().equals("META-INF/pustefix-module.xml");
    }

    private static Properties getProperties(Node webapp) throws IOException {
        Node src;

        src = webapp.join(PROPERTIES);
        if (!src.exists()) {
            // TODO: dump this compatibility check as soon as I have ITs with new wars
            src = webapp.join("WEB-INF/lavendel.properties");
        }
        return src.readProperties();
    }

    public static PustefixModule create(Filter filter, Node webapp) throws IOException {
        ProjectConfig config;

        try (InputStream src = webapp.join("WEB-INF/project.xml").createInputStream()) {
            config = JAXB.unmarshal(src, ProjectConfig.class);
        }
        return new PustefixModule(filter, config, webapp);
    }

    //--

    private final ProjectConfig config;
    private final Node webapp;

    public PustefixModule(Filter filter, ProjectConfig config, Node webapp) throws IOException {
        super(filter, View.WEB, true, "");

        this.config = config;
        this.webapp = webapp;
    }

    public Iterator<Resource> iterator() {
        try {
            return PustefixResourceIterator.create(this, webapp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isPublicResource(String resourceName) {
        if (resourceName.startsWith("WEB-INF")) {
            return false;
        }

        for (String path : config.getApplication().getStatic().getPath()) {
            if (resourceName.startsWith(path)) {
                return true;
            }
        }

        return false;
    }

    public String getProjectName() {
        return config.getProject().getName();
    }

}
