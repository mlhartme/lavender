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
import net.oneandone.lavender.config.Alias;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.Properties;
import net.oneandone.lavender.config.Target;
import net.oneandone.lavender.index.Distributor;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class War extends Base {
    private final FileNode war;
    private final String idxName;
    private final Map<String, Target> targets;
    private String nodes;

    public War(Globals globals, FileNode war, String idxName, List<String> targetKeyValues) throws IOException {
        super(globals);

        int idx;
        String docrootName;
        String clusterName;
        String aliasName;
        Cluster cluster;
        Docroot docroot;
        Alias alias;

        this.war = war;
        this.idxName = idxName;
        this.targets = new HashMap<>();
        for (String keyvalue : targetKeyValues) {
            idx = keyvalue.indexOf('=');
            if (idx == -1) {
                throw new ArgumentException("<type>=<cluster> expected, got " + keyvalue);
            }
            docrootName = keyvalue.substring(0, idx);
            clusterName = keyvalue.substring(idx + 1);
            idx = clusterName.indexOf('/');
            if (idx == -1) {
                aliasName = null;
            } else {
                aliasName = clusterName.substring(idx + 1);
                clusterName = clusterName.substring(0, idx);
            }
            cluster = globals.net().get(clusterName);
            docroot = cluster.docroot(docrootName);
            alias = aliasName == null ? docroot.aliases().get(0) : docroot.alias(aliasName);
            if (Docroot.WEB.equals(docrootName)) {
                nodes = alias.nodesFile();
            }
            targets.put(docrootName, new Target(cluster, docroot, alias));

        }
    }

    public void run() throws IOException, SAXException, XmlException {
        FileNode tmp;
        FileNode cache;
        FileNode outputNodesFile;
        WarEngine engine;
        Map<String, Distributor> distributors;
        Properties properties;

        if (targets.isEmpty()) {
            throw new ArgumentException("missing targets");
        }
        if (nodes == null) {
            throw new ArgumentException("missing web target");
        }
        war.checkFile();
        tmp = war.getWorld().getTemp();
        outputNodesFile = tmp.createTempFile();
        properties = globals.properties();
        try (Pool pool = globals.pool()) {
            distributors = distributors(pool);
            cache = globals.lockedCache();
            try {
                engine = new WarEngine(cache, distributors, idxName, properties.svnUsername, properties.svnPassword,
                        war, outputNodesFile, nodes);
                engine.run();
            } finally {
                properties.unlockCache();
            }
        }
        outputNodesFile.deleteFile();
    }

    private Map<String, Distributor> distributors(Pool pool) throws IOException {
        Map<String, Distributor> result;

        result = new HashMap<>();
        for (Map.Entry<String, Target> entry : targets.entrySet()) {
            result.put(entry.getKey(), entry.getValue().open(pool, idxName));
        }
        return result;
    }
}
