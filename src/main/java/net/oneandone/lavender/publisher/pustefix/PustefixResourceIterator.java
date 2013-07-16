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
import net.oneandone.sushi.io.Buffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PustefixResourceIterator implements Iterator<Resource> {
    private File war;
    private PustefixProjectConfig config;

    private ZipInputStream warInputStream;

    private ZipInputStream moduleInputStream;
    private PustefixModuleConfig moduleConfig;

    private Resource next;

    private final Buffer buffer;

    public PustefixResourceIterator(File war) {
        this.war = war;
        this.config = new PustefixProjectConfig(war);
        this.buffer = new Buffer();
    }

    public boolean hasNext() {

        try {
            if (next != null) {
                return true;
            }

            if (warInputStream == null) {
                warInputStream = new ZipInputStream(new FileInputStream(war));
            }

            do {
                ZipEntry entry;

                if (moduleConfig != null) {

                    while ((entry = moduleInputStream.getNextEntry()) != null) {
                        String name = entry.getName();

                        if (!entry.isDirectory() && moduleConfig.isPublicResource(name)) {
                            next = createModuleResource(name);
                            return true;
                        }
                    }

                    moduleConfig = null;
                    moduleInputStream = null;
                }

                while ((entry = warInputStream.getNextEntry()) != null) {
                    String name = entry.getName();

                    if (!entry.isDirectory() && config.isPublicResource(name)) {
                        next = createProjectResource(name);
                        return true;
                    }

                    if (config.isModule(name)) {
                        moduleConfig = config.getModuleConfig(name);
                        moduleInputStream = new ZipInputStream(warInputStream);

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

    private Resource createProjectResource(String path) throws IOException {
        String folderName = config.getProjectName();
        String[] splitted = path.split("/");
        if (splitted.length > 2 && splitted[0].equals("modules")) {
            folderName = splitted[1];
        }
        return createResource(warInputStream, path, folderName);
    }

    private Resource createModuleResource(String name) throws IOException {
        return createResource(moduleInputStream, moduleConfig.getPath(name), moduleConfig.getModuleName());
    }

    private Resource createResource(InputStream in, String path, String folderName) throws IOException {
        ByteArrayOutputStream dest;
        byte[] data;

        dest = new ByteArrayOutputStream();
        buffer.copy(in, dest);
        data = dest.toByteArray();
        return new Resource(data, path, folderName);
    }
}
