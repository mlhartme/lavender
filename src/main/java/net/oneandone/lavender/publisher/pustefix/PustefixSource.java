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
package net.oneandone.lavender.publisher.pustefix;

import net.oneandone.lavender.publisher.Source;
import net.oneandone.lavender.publisher.Resource;
import net.oneandone.lavender.publisher.config.Filter;
import net.oneandone.lavender.publisher.pustefix.project.ProjectConfig;
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
public class PustefixSource extends Source {
    private static final Logger LOG = LoggerFactory.getLogger(Source.class);

    private static final List<String> DEFAULT_INCLUDE_EXTENSIONS = new ArrayList<>(Arrays.asList(
            "gif", "png", "jpg", "jpeg", "ico", "swf", "css", "js"));

    private static final String PROPERTIES = "WEB-INF/lavender.properties";

    public static List<Source> fromWebapp(Node webapp, String svnUsername, String svnPassword) throws IOException {
        List<Source> result;
        Properties properties;
        PustefixSource ps;
        PustefixModuleConfig mc;

        LOG.trace("scanning " + webapp);
        result = new ArrayList<>();
        properties = getProperties(webapp);
        ps = create(Filter.forProperties(properties, "pustefix", DEFAULT_INCLUDE_EXTENSIONS), webapp);
        result.add(ps);
        for (Node jar : webapp.find("WEB-INF/lib/*.jar")) {
            mc = loadModuleXml(ps, jar);
            if (mc != null) {
                result.add(new JarSource(ps.getFilter(), mc, jar));
            }
        }
        for (SvnSourceConfig config : SvnSourceConfig.parse(properties)) {
            LOG.info("adding svn source " + config.folder);
            result.add(config.create(webapp.getWorld(), svnUsername, svnPassword));
        }
        return result;
    }

    private static PustefixModuleConfig loadModuleXml(PustefixSource source, Node jar) throws IOException {
        ZipInputStream jarInputStream;
        ZipEntry jarEntry;

        jarInputStream = new ZipInputStream(jar.createInputStream());
        while ((jarEntry = jarInputStream.getNextEntry()) != null) {
            if (isModuleXml(jarEntry)) {
                try {
                    return new PustefixModuleConfig(source, jarInputStream);
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

    public static PustefixSource create(Filter filter, Node webapp) throws IOException {
        ProjectConfig config;

        try (InputStream src = webapp.join("WEB-INF/project.xml").createInputStream()) {
            config = JAXB.unmarshal(src, ProjectConfig.class);
        }
        return new PustefixSource(filter, config, webapp);
    }

    //--

    private final ProjectConfig config;
    private final Node webapp;

    public PustefixSource(Filter filter, ProjectConfig config, Node webapp) throws IOException {
        super(filter, DEFAULT_STORAGE, true, "");

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
