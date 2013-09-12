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
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarStreamResourceIterator implements Iterator<Resource> {
    private final World world;
    private final ZipInputStream moduleInputStream;
    private final ApplicationModuleConfig moduleConfig;

    // iterating data
    private Resource next;

    public JarStreamResourceIterator(ApplicationModuleConfig moduleConfig, Node file) throws IOException {
        this.world = file.getWorld();
        this.moduleConfig = moduleConfig;
        this.moduleInputStream = new ZipInputStream(file.createInputStream());
    }

    public boolean hasNext() {
        ZipEntry entry;
        String path;
        byte[] data;

        try {
            if (next != null) {
                return true;
            }
            while ((entry = moduleInputStream.getNextEntry()) != null) {
                path = entry.getName();
                if (!entry.isDirectory() && moduleConfig.isPublicResource(path)) {
                    data = world.getBuffer().readBytes(moduleInputStream);
                    next = DefaultResource.forBytes(data, moduleConfig.getPath(path));
                    return true;
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
