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
package net.oneandone.lavender.cli;

import net.oneandone.inline.ArgumentException;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Distributor;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.NodeModule;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class File extends Base {
    private final String prefix;
    private final FileNode archive;
    private final String indexName;
    private final String docrootName;
    private final Cluster cluster;

    public File(Globals globals, String prefix, FileNode archive, String clusterName, String docrootName, String indexName) throws IOException {
        super(globals);
        this.prefix = prefix;
        this.archive = archive.checkExists();
        this.indexName = indexName;
        this.docrootName = docrootName;
        this.cluster = globals.net().get(clusterName);
    }

    public void run() throws IOException {
        final Node<?> exploded;
        Docroot docroot;
        Filter filter;
        Module module;
        Distributor distributor;
        long changed;
        Index index;
        FileNode cacheroot;

        if (archive.isFile()) {
            try {
                exploded = archive.openZip();
            } catch (IOException e) {
                throw new ArgumentException(archive + ": cannot open zip archive: " + e.getMessage(), e);
            }
        } else {
            exploded = archive;
        }
        docroot = cluster.docroot(docrootName);
        filter = new Filter();
        filter.includeAll();
        filter.predicate(Predicate.FILE);
        module = new NodeModule(Module.TYPE, indexName, false, "", prefix, filter) {
            @Override
            protected Map<String, Node> loadEntries() throws Exception {
                Map<String, Node> result;

                result = new HashMap<>();
                for (Node node : exploded.find(filter)) {
                    result.put(node.getPath(), node);
                }
                return result;
            }
        };

        try (Pool pool = globals.pool()) {
            cacheroot = globals.lockedCacheroot();
            try {
                distributor = Distributor.open(cacheroot, cluster.connect(pool), docroot, indexName);
                changed = distributor.publish(module);
                index = distributor.close();
            } finally {
                globals.properties().unlockCacheroot();
            }
        }
        console.info.println("done: " + changed + "/" + index.size() + " files changed");
    }
}
