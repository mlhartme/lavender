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

import net.oneandone.lavender.publisher.pustefix.project.ProjectConfig;
import net.oneandone.sushi.fs.Node;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;

/** Project.xml and more. */
public class PustefixProjectConfig {
    private ProjectConfig project;

    public PustefixProjectConfig(Node webapp) throws IOException, JAXBException {
        loadProjectXml(webapp.join("WEB-INF/project.xml"));
    }

    /**
     * Checks if the given resource is public.
     * @param resourceName
     *            the resource name
     * @return true if the resource is public
     */
    public boolean isPublicResource(String resourceName) {
        if (resourceName.startsWith("WEB-INF")) {
            return false;
        }

        for (String path : project.getApplication().getStatic().getPath()) {
            if (resourceName.startsWith(path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the project name.
     * @return the project name
     */
    public String getProjectName() {
        return project.getProject().getName();
    }

    //--

    private void loadProjectXml(Node node) throws JAXBException, IOException {
        try (InputStream src = node.createInputStream()) {
            project = JAXB.unmarshal(src, ProjectConfig.class);
        }
    }
}
