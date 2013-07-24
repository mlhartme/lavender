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
import net.oneandone.lavender.index.Resource;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Predicate;

import java.io.IOException;
import java.util.Iterator;

public class JarModule extends Module {
    private final PustefixModuleConfig config;
    private final Node jar;

    public JarModule(Filter filter, PustefixModuleConfig config, Node jar) {
        super(filter, DEFAULT_STORAGE, true, "");
        this.config = config;
        this.jar = jar;
    }

    public Iterator<Resource> iterator() {
        try {
            if (jar instanceof FileNode) {
                return new JarFileResourceIterator(config, (FileNode) jar, jar.getWorld().filter().includeAll().predicate(Predicate.FILE));
            } else {
                return new JarStreamResourceIterator(config, jar);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}