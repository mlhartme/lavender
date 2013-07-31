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
import net.oneandone.sushi.fs.svn.SvnNode;

import java.util.Iterator;
import java.util.List;

/** Extracts resources from svn */
public class SvnModule extends Module {
    private final SvnNode root;
    private final List<SvnFile> resources;
    private final String folder;

    public SvnModule(Filter filter, String type, SvnNode root, boolean lavendelize, String pathPrefix,
                     List<SvnFile> resources, String folder) {
        super(filter, type, lavendelize, pathPrefix);
        this.root = root;
        this.resources = resources;
        this.folder = folder;
    }

    public Iterator<Resource> iterator() {
        final Iterator<SvnFile> base;

        base = resources.iterator();
        return new Iterator<Resource>() {
            public boolean hasNext() {
                return base.hasNext();
            }

            public Resource next() {
                SvnNode node;
                SvnFile file;

                file = base.next();
                node = root.join(file.path);
                return Resource.forNode(node, file.path, folder);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
