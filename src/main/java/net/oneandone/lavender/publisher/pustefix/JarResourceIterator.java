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
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.io.Buffer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarResourceIterator implements Iterator<Resource> {
    private final World world;
    private final ZipInputStream moduleInputStream;
    private final PustefixModuleConfig moduleConfig;
    private final List<Node> moduleJarFiles;

    // iterating data
    private int nextModuleJarFile;
    private Resource next;

    public JarResourceIterator(PustefixModuleConfig moduleConfig, Node file, Filter fileFilter) throws IOException {
        this.world = file.getWorld();
        this.moduleConfig = moduleConfig;

        if (file instanceof FileNode) {
            moduleInputStream = null;
            moduleJarFiles = ((FileNode) file).openZip().find(fileFilter);
            nextModuleJarFile = 0;
        } else {
            moduleInputStream = new ZipInputStream(file.createInputStream());
            moduleJarFiles = null;
        }
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
            if (moduleInputStream != null) {
                while ((entry = moduleInputStream.getNextEntry()) != null) {
                    path = entry.getName();
                    if (!entry.isDirectory() && moduleConfig.isPublicResource(path)) {
                        data = world.getBuffer().readBytes(moduleInputStream);
                        next = new Resource(world.memoryNode(data), moduleConfig.getPath(path), moduleConfig.getModuleName());
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
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Resource next() {
        Resource result;

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        result = next;
        next = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
