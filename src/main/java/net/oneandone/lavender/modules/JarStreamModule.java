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

public class JarStreamModule extends Module {
    private final JarModuleConfig config;
    private final Node jar;

    public JarStreamModule(Filter filter, String type, JarModuleConfig config, Node jar) {
        super(filter, type, config.getModuleName(), true, "");
        this.config = config;
        this.jar = jar;
    }

    public Iterator<Resource> iterator() {
        try {
            return new JarStreamResourceIterator(config, jar);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Resource probeIncluded(String path) {
        // that's ok because JarStreamModules are only used when publishing wars -- and that doesn't need probing
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveCaches() {
        // nothing to do
    }
}
