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
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.SystemProperties;
import net.oneandone.lavender.modules.Distributor;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;

public class War extends Base {
    private final FileNode war;
    private final String indexName;
    private final Cluster cluster;
    private final Docroot docroot;
    private String nodes;

    public War(Globals globals, FileNode war, String clusterName, String docrootName, String indexName) throws IOException, URISyntaxException {
        super(globals);

        this.war = war.checkFile();
        this.indexName = indexName;
        this.cluster = globals.network().get(clusterName);
        this.docroot = cluster.docroot(docrootName);
        this.nodes = docroot.nodesFile();
    }

    public void run() throws IOException, SAXException, XmlException, URISyntaxException {
        FileNode tmp;
        FileNode cacheroot;
        FileNode outputNodesFile;
        WarEngine engine;
        SystemProperties properties;

        tmp = war.getWorld().getTemp();
        outputNodesFile = tmp.createTempFile();
        properties = globals.properties();
        try (Pool pool = globals.pool()) {
            cacheroot = globals.cacheroot();
            engine = new WarEngine(cacheroot, Distributor.open(cacheroot, cluster.connect(pool), docroot, indexName), properties.secrets.get("svn"), war, outputNodesFile, nodes);
            engine.run();
        }
        outputNodesFile.deleteFile();
    }
}
