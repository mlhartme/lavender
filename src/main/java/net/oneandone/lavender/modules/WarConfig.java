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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.xml.Selector;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WarConfig {
    public static WarConfig fromXml(Node webapp) throws IOException {
        String path;
        Element root;
        Selector selector;
        List<String> statics;

        try {
            root = webapp.join("WEB-INF/project.xml").readXml().getDocumentElement();
            selector = webapp.getWorld().getXml().getSelector();
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
            return new WarConfig(statics);
        } catch (SAXException e) {
            throw new IOException("cannot load project descriptor: " + e);
        }
    }

    //--

    private final List<String> statics;

    public WarConfig(List<String> statics) {
        this.statics = statics;
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
}
