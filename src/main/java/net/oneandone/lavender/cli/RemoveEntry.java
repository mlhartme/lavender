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

import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Connection;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.Properties;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RemoveEntry extends Base {
    @Value(name = "cluster", position = 1)
    private String clusterName;

    private final List<String> originalPaths = new ArrayList<>();

    @Remaining(name = "originalPaths")
    public void path(String arg) {
        originalPaths.add(arg);
    }


    public RemoveEntry(Console console, Properties properties, Net net) {
        super(console, properties, net);
    }

    @Override
    public void invoke() throws IOException {
        Cluster cluster;
        Node docrootNode;

        cluster = net.get(clusterName);
        try (Pool pool = pool()) {
            for (Docroot docroot : cluster.docroots()) {
                for (Connection connection : cluster.connect(pool)) {
                    console.info.println(connection.getHost() + " " + docroot.aliases().get(0).getName());
                    docrootNode = docroot.node(connection);
                    if (docrootNode.exists()) {
                        remove(connection, docroot);
                    }
                }
            }
        }
    }

    private void remove(Connection connection, Docroot docroot) throws IOException {
        Node allFile;
        Index all;
        Index index;
        Label label;
        boolean allModified;
        boolean modified;

        allModified = false;
        allFile = docroot.index(connection, Index.ALL_IDX);
        all = Index.load(allFile);
        for (Node file : docroot.indexList(connection)) {
            index = Index.load(file);
            modified = false;
            for (String originalPath : originalPaths) {
                label = index.lookup(originalPath);
                if (label != null) {
                    if (!index.removeEntryOpt(originalPath)) {
                        throw new IllegalStateException(originalPath);
                    }
                    if (!all.removeReferenceOpt(label.getLavendelizedPath())) {
                        throw new IllegalStateException(label.toString());
                    }
                    modified = true;
                }
            }
            if (modified) {
                console.info.println("M " + file.getName());
                index.save(file);
                allModified = true;
            }
        }
        if (allModified) {
            all.save(allFile);
        }
    }

}
