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

import net.oneandone.lavender.config.Filter;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarModule extends Module {
    /** To properly make jars available as a module, I have to load them into memory when the jar is itself packaged into a war. */
    public static Object[] fromJar(Filter filter, String type, JarModuleConfig config, Node jar) throws IOException {
        World world;
        ZipEntry entry;
        String path;
        ZipInputStream src;
        Node root;
        Node child;
        boolean isProperty;
        Node propertyNode;
        List<Node> files;

        world = jar.getWorld();
        root = world.getMemoryFilesystem().root().node(UUID.randomUUID().toString(), null).mkdir();
        src = new ZipInputStream(jar.createInputStream());
        propertyNode = null;
        files = new ArrayList<>();
        while ((entry = src.getNextEntry()) != null) {
            path = entry.getName();
            if (!entry.isDirectory()) {
                isProperty = WarModule.PROPERTIES.equals(path);
                if (config.isPublicResource(path) || isProperty) {
                    child = root.join(path);
                    child.getParent().mkdirsOpt();
                    world.getBuffer().copy(src, child);
                    if (isProperty) {
                        propertyNode = child;
                    } else {
                        files.add(child);
                    }
                }
            }
        }
        return new Object[] { new JarModule(filter, type, config, root, files), propertyNode };
    }

    private final JarModuleConfig config;
    private final Node exploded;
    private final List<Node> files;

    public JarModule(Filter filter, String type, JarModuleConfig config, Node exploded, List<Node> files) throws IOException {
        super(filter, type, config.getModuleName(), true, "");
        this.config = config;
        this.exploded = exploded;
        this.files = files;
    }

    public Iterator<Resource> iterator() {
        return new JarResourceIterator(exploded, config, files);
    }

    // TODO: expensive
    public Resource probeIncluded(String path) {
        for (Resource resource : this) {
            if (path.equals(resource.getPath())) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public void saveCaches() {
        // nothing to do
    }
}
