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
import net.oneandone.lavender.publisher.svn.SvnSourceConfig;
import net.oneandone.sushi.fs.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
        PustefixProjectConfig pc;
        PustefixSource ps;

        LOG.trace("scanning " + webapp);
        result = new ArrayList<>();
        properties = getConfig(webapp);
        ps = PustefixSource.forProperties(webapp, properties);
        result.add(ps);
        try {
            pc = new PustefixProjectConfig(webapp);
        } catch (JAXBException e) {
            throw new IOException("cannot load pustefix configuration: " + e.getMessage(), e);
        }
        for (Map.Entry<String, PustefixModuleConfig> entry : pc.getModules().entrySet()) {
            result.add(new JarSource(ps.getFilter(), entry.getValue(), webapp.join(entry.getKey())));
        }
        for (SvnSourceConfig config : SvnSourceConfig.parse(properties)) {
            LOG.info("adding svn source " + config.folder);
            result.add(config.create(webapp.getWorld(), svnUsername, svnPassword));
        }
        return result;
    }

    private static Properties getConfig(Node webapp) throws IOException {
        Node src;

        src = webapp.join(PROPERTIES);
        if (!src.exists()) {
            // TODO: dump this compatibility check as soon as I have ITs with new wars
            src = webapp.join("WEB-INF/lavendel.properties");
        }
        return src.readProperties();
    }

    public static PustefixSource forProperties(Node webapp, Properties properties) {
        return new PustefixSource(Filter.forProperties(properties, "pustefix", DEFAULT_INCLUDE_EXTENSIONS), webapp);
    }

    private final Node webapp;

    public PustefixSource(Filter filter, Node webapp) {
        super(filter, DEFAULT_STORAGE, true, "");
        this.webapp = webapp;
    }

    public Iterator<Resource> iterator() {
        try {
            return PustefixResourceIterator.create(webapp);
        } catch (IOException | JAXBException e) {
            throw new IllegalStateException(e);
        }
    }
}
