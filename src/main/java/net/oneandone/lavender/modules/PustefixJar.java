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
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A pustefix jar is a jar with a pustefix module descriptor. If it also has module properties,
 * you can instantiate a Lavender module from it (more precisely: an embedded module,
 * i.e. a module that loads all resources from the underlying jar)
 */
public class PustefixJar {
    private static final String PUSTEFIX_MODULE_XML = "META-INF/pustefix-module.xml";
    private static final String RESOURCE_INDEX = "META-INF/pustefix-resource.index";
    public static final String POMINFO_PROPERTIES = "META-INF/pominfo.properties";

    /** @return null if not a pustefix jar */
    public static PustefixJar forNodeOpt(boolean prod, Node jar, WarConfig rootConfig) throws IOException {
        PustefixJar result;

        if (jar instanceof FileNode) {
            result = forFileNodeOpt(prod, (FileNode) jar, rootConfig);
        } else {
            if (!prod) {
                throw new UnsupportedOperationException("live mechanism not supported for jar streams");
            }
            result = forOtherNodeOpt(jar, rootConfig);
        }
        if (result != null) {
            if (result.moduleProperties != null && !result.hasResourceIndex) {
                throw new IOException("missing resource index: " + result.config.getModuleName());
            }
        }
        return result;
    }

    /** Loads resources from the jar into memory. */
    private static PustefixJar forOtherNodeOpt(Node jar, WarConfig warConfig) throws IOException {
        PustefixJarConfig config;
        ModuleProperties moduleProperties;
        boolean hasResourceIndex;

        Node[] loaded;
        Node propertyNode;

        loaded = loadStreamNodes(jar, PUSTEFIX_MODULE_XML, ModuleProperties.MODULE_PROPERTIES, POMINFO_PROPERTIES, RESOURCE_INDEX);
        if (loaded[0] == null) {
            return null;
        }
        config = PustefixJarConfig.load(loaded[0], warConfig);
        propertyNode = loaded[1];
        if (propertyNode == null) {
            moduleProperties = null;
        } else {
            if (loaded[2] == null) {
                throw new IOException("missing pominfo.properties in jar " + jar);
            }
            moduleProperties = ModuleProperties.loadModule(true, propertyNode, loaded[2]);
        }
        hasResourceIndex = loaded[3] != null;
        if (moduleProperties == null && hasResourceIndex) {
            // ok - we have a recent parent pom without lavender properties
            // -> the has not enabled lavender for this module
            return null;
        }
        return new PustefixJar(config, moduleProperties, hasResourceIndex);
    }

    private static PustefixJar forFileNodeOpt(boolean prod, FileNode jarOrig, WarConfig warConfig) throws IOException {
        PustefixJarConfig config;
        ModuleProperties moduleProperties;
        boolean hasResourceIndex;

        Node exploded;
        Node configFile;

        exploded = jarOrig.openJar();
        configFile = exploded.join(PUSTEFIX_MODULE_XML);
        if (!configFile.exists()) {
            return null;
        }
        config = PustefixJarConfig.load(configFile, warConfig);
        moduleProperties = ModuleProperties.loadModuleOpt(prod, exploded);
        if (moduleProperties == null) {
            return null;
        }
        hasResourceIndex = exploded.join(RESOURCE_INDEX).exists();
        return new PustefixJar(config, moduleProperties, hasResourceIndex);
    }

    //--

    /** To properly make jars available as a module, I have to load them into memory when the jar is itself packaged into a war. */
    private static Node[] loadStreamNodes(Node jar, String... names) throws IOException {
        World world;
        int count;
        Node[] result;
        ZipEntry entry;
        String path;
        Node dest;
        int idx;

        world = jar.getWorld();
        count = 0;
        result = new Node[names.length];
        try (ZipInputStream src = new ZipInputStream(jar.newInputStream())) {
            while (true) {
                entry = src.getNextEntry();
                if (entry == null) {
                    break;
                }
                path = entry.getName();
                idx = indexOf(names, path);
                if (idx != -1) {
                    count++;
                    dest = world.memoryNode();
                    result[idx] = dest;
                    world.getBuffer().copy(src, dest);
                    if (count == names.length) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static int indexOf(String[] all, String element) {
        for (int i = 0; i < all.length; i++) {
            if (element.equals(all[i])) {
                return i;
            }
        }
        return -1;
    }

    //--

    public final PustefixJarConfig config;
    public final ModuleProperties moduleProperties;
    public final boolean hasResourceIndex;

    public PustefixJar(PustefixJarConfig config, ModuleProperties moduleProperties, boolean hasResourceIndex) {
        this.config = config;
        this.moduleProperties = moduleProperties;
        this.hasResourceIndex = hasResourceIndex;
    }
}
