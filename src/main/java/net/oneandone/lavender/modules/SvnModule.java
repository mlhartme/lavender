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
import net.oneandone.lavender.index.Index;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.svn.SvnNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** Extracts resources from svn */
public class SvnModule extends Module {
    private final SvnNode root;

    // TODO: dump
    private final List<Resource> resources;
    private final String folder;

    private final Index index;
    private final Node indexFile;

    public SvnModule(Filter filter, String type, Index index, Node indexFile, SvnNode root, boolean lavendelize, String pathPrefix,
                     List<Resource> resources, String folder) {
        super(filter, type, lavendelize, pathPrefix);
        this.root = root;
        this.resources = resources;
        this.folder = folder;

        this.index = index;
        this.indexFile = indexFile;
    }

    public Iterator<Resource> iterator() {
        return resources.iterator();
    }

    public Index index() {
        return index;
    }

    public String uri() {
        return root.getURI().toString();
    }

    public void saveCaches() throws IOException {
        index.save(indexFile);
    }


}
