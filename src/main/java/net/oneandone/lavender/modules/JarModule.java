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
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Predicate;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class JarModule extends Module {
    private final JarModuleConfig config;
    private final Node jar;

    public JarModule(Filter filter, String type, JarModuleConfig config, Node jar) {
        super(filter, type, config.getModuleName(), true, "");
        this.config = config;
        this.jar = jar;
    }

    public Iterator<Resource> iterator() {
        List<Node> files;
        try {
            if (jar instanceof FileNode) {
                files = ((FileNode) jar).openZip().find(jar.getWorld().filter().includeAll().predicate(Predicate.FILE));
                return new JarFileResourceIterator(config, files);
            } else {
                return new JarStreamResourceIterator(config, jar);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
