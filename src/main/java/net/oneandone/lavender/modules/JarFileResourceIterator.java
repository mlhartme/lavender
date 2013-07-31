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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class JarFileResourceIterator implements Iterator<Resource> {
    private final PustefixModuleConfig moduleConfig;
    private final List<Node> moduleJarFiles;

    // iterating data
    private int nextModuleJarFile;
    private Resource next;

    public JarFileResourceIterator(PustefixModuleConfig moduleConfig, FileNode file, Filter fileFilter) throws IOException {
        this.moduleConfig = moduleConfig;
        this.moduleJarFiles = file.openZip().find(fileFilter);
        this.nextModuleJarFile = 0;
    }

    public boolean hasNext() {
        Node file;
        String path;

        if (next != null) {
            return true;
        }
        while (nextModuleJarFile < moduleJarFiles.size()) {
            file = moduleJarFiles.get(nextModuleJarFile);
            nextModuleJarFile++;
            path = file.getPath();
            if (moduleConfig.isPublicResource(path)) {
                try {
                    next = Resource.forNode(file, moduleConfig.getPath(path), moduleConfig.getModuleName());
                } catch (IOException e) {
                    throw new RuntimeException("TODO", e);
                }
                return true;
            }
        }
        return false;
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
