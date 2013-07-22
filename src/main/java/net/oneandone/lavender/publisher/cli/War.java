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
package net.oneandone.lavender.publisher.cli;

import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.publisher.Distributor;
import net.oneandone.lavender.config.Alias;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.publisher.WarEngine;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Settings;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class War extends Base {
    public static Map<String, Distributor> createDefaultStorages(
            World world, Cluster cdn, Cluster flashEu, Cluster flashUs, String indexName) throws IOException {
        Map<String, Distributor> storages;

        storages = new HashMap<>();
        storages.put(Module.DEFAULT_STORAGE, Distributor.forCdn(world, cdn, (Docroot) cdn.alias("fix")[0], indexName));
        String flashIdx = Strings.replace(Strings.replace(indexName, "-a.idx", ""), "-b.idx", "") + ".idx";
        storages.put("flash-eu", Distributor.forCdn(world, flashEu, flashEu.docroots.get(0), flashIdx));
        storages.put("flash-us", Distributor.forCdn(world, flashUs, flashUs.docroots.get(0), flashIdx));
        return storages;
    }

    //--

    @Value(name = "cluster", position = 1)
    private String clusterAlias;

    @Value(name = "inputWar", position = 2)
    private FileNode inputWar;

    @Value(name = "outputWar", position = 3)
    private FileNode outputWar;

    @Value(name = "idxName", position = 4)
    private String indexName;


    public War(Console console, Settings settings, Net net) {
        super(console, settings, net);
    }

    @Override
    public void invoke() throws IOException {
        FileNode tmp;
        Index outputIndex;
        FileNode outputNodesFile;
        WarEngine engine;
        Map<String, Distributor> storages;
        Cluster cluster;
        Alias alias;
        Object[] t;
        int idx;

        inputWar.checkFile();
        outputWar.checkNotExists();

        tmp = inputWar.getWorld().getTemp();
        outputIndex = new Index();
        outputNodesFile = tmp.createTempFile();
        idx = clusterAlias.indexOf("/");
        if (idx == -1) {
            cluster = net.cluster(clusterAlias);
            alias =  cluster.docroots.get(0).aliases.get(0);
        } else {
            cluster = net.cluster(clusterAlias.substring(0, idx));
            t = cluster.alias(clusterAlias.substring(idx + 1));
            alias = (Alias) t[1];
        }
        storages = createDefaultStorages(console.world, cluster, net.cluster("flash-eu"), net.cluster("flash-us"), indexName);
        engine = new WarEngine(settings.svnUsername, settings.svnPassword,
                inputWar, outputWar, storages, outputIndex, outputNodesFile, alias.nodesFile());
        engine.run();
        outputNodesFile.deleteFile();
    }
}
