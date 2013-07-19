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

import net.oneandone.lavender.publisher.Resource;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PustefixResourceIterator implements Iterator<Resource> {
    private final PustefixProjectConfig config;
    private final Node webapp;
    private List<Node> files;

    private final Filter fileFilter;

    // iterating data

    private Resource next;

    private int nextFile;
    private ZipInputStream moduleInputStream;
    private PustefixModuleConfig moduleConfig;
    private List<Node> moduleJarFiles;
    private int nextModuleJarFile;


    public static PustefixResourceIterator create(Node webapp) throws IOException, JAXBException {
        PustefixProjectConfig config;
        Filter filter;

        config = new PustefixProjectConfig(webapp);
        filter = webapp.getWorld().filter().include("**/*").predicate(Predicate.FILE);
        return new PustefixResourceIterator(config, webapp, webapp.find(filter), filter);
    }


    public PustefixResourceIterator(PustefixProjectConfig config, Node webapp, List<Node> files, Filter fileFilter) throws IOException, JAXBException {
        this.config = config;
        this.webapp = webapp;
        this.files = files;
        this.nextFile = 0;

        this.fileFilter = fileFilter;
    }

    public boolean hasNext() {
        Node file;
        ZipEntry entry;
        String path;
        byte[] data;

        try {
            if (next != null) {
                return true;
            }
            do {
                if (moduleConfig != null) {
                    if (moduleInputStream != null) {
                        while ((entry = moduleInputStream.getNextEntry()) != null) {
                            path = entry.getName();
                            if (!entry.isDirectory() && moduleConfig.isPublicResource(path)) {
                                data = webapp.getWorld().getBuffer().readBytes(moduleInputStream);
                                next = new Resource(webapp.getWorld().memoryNode(data), moduleConfig.getPath(path), moduleConfig.getModuleName());
                                return true;
                            }
                        }
                    } else {
                        while (nextModuleJarFile < moduleJarFiles.size()) {
                            file = moduleJarFiles.get(nextModuleJarFile);
                            nextModuleJarFile++;
                            path = file.getPath();
                            if (moduleConfig.isPublicResource(path)) {
                                next = new Resource(file, moduleConfig.getPath(path), moduleConfig.getModuleName());
                                return true;
                            }
                        }
                    }
                    moduleConfig = null;
                    moduleInputStream = null;
                    moduleJarFiles = null;
                }

                while (nextFile < files.size()) {
                    file = files.get(nextFile++);
                    path = file.getRelative(webapp);
                    if (config.isPublicResource(path)) {
                        String folderName = config.getProjectName();
                        String[] splitted = path.split("/");
                        if (splitted.length > 2 && splitted[0].equals("modules")) {
                            folderName = splitted[1];
                        }
                        next = new Resource(file, path, folderName);
                        return true;
                    }
                    if (config.isModule(path)) {
                        moduleConfig = config.getModuleConfig(path);
                        if (file instanceof FileNode) {
                            moduleInputStream = null;
                            moduleJarFiles = ((FileNode) file).openZip().find(fileFilter);
                            nextModuleJarFile = 0;
                        } else {
                            moduleInputStream = new ZipInputStream(file.createInputStream());
                            moduleJarFiles = null;
                        }
                        break;
                    }
                }
            } while (moduleConfig != null);

            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Resource result = next;
        next = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
