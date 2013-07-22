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
import net.oneandone.sushi.fs.Node;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Iterates static resources from a Pustefix application. Valid static resource path are defined in WEB-INF/project.xml.
 * Resources can be found in the WAR or in nested JARs.
 */
public class PustefixSource extends Source {
    private static final List<String> DEFAULT_INCLUDE_EXTENSIONS = new ArrayList<>(Arrays.asList(
            "gif", "png", "jpg", "jpeg", "ico", "swf", "css", "js"));

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
