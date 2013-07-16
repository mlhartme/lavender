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

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Project.xml and more. */
public class PustefixProjectConfig {
    private ProjectConfig project;
    private Map<String, PustefixModuleConfig> modules = new HashMap<>();

    public PustefixProjectConfig(File war) {
        load(war);
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

    public Map<String, PustefixModuleConfig> getModules() {
        return modules;
    }

    public PustefixModuleConfig getModuleConfig(String name) {
        return modules.get(name);
    }

    public boolean isModule(String name) {
        return modules.containsKey(name);
    }

    //--

    private  void load(File war) {
        try {
            ZipInputStream warInputStream = new ZipInputStream(new FileInputStream(war));
            ZipEntry warEntry;
            while ((warEntry = warInputStream.getNextEntry()) != null) {
                if (isProjectXml(warEntry)) {
                    loadProjectXml(warInputStream);
                }
                if (isJarEntry(warEntry)) {
                    loadModuleXml(warEntry, warInputStream);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadProjectXml(InputStream warInputStream) throws JAXBException, IOException {
        // Use a shield to prevent the original stream from being closed
        // because JAXB.unmarshall() calls close() on the stream
        project = JAXB.unmarshal(PustefixModuleConfig.doNotClose(warInputStream), ProjectConfig.class);
    }

    private void loadModuleXml(ZipEntry warEntry, ZipInputStream warInputStream) throws JAXBException, IOException {
        ZipInputStream jarInputStream = new ZipInputStream(warInputStream);
        ZipEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextEntry()) != null) {
            if (isModuleXml(jarEntry)) {
                PustefixModuleConfig config = new PustefixModuleConfig(this, jarInputStream);
                modules.put(warEntry.getName(), config);
            }
        }
    }

    private boolean isProjectXml(ZipEntry entry) {
        return entry.getName().equals("WEB-INF/project.xml");
    }

    private boolean isJarEntry(ZipEntry entry) {
        return entry.getName().startsWith("WEB-INF/lib/") && entry.getName().endsWith(".jar");
    }

    private boolean isModuleXml(ZipEntry entry) {
        return entry.getName().equals("META-INF/pustefix-module.xml");
    }
}
