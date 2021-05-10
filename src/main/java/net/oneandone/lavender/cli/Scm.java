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
import net.oneandone.lavender.config.HostProperties;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Distributor;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.ScmProperties;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Scm extends Base {
    private final String prefix;
    private final String scm;
    private final Cluster cluster;
    private final String docrootName;
    private final String indexName;

    public Scm(Globals globals, String prefix, String scm, String clusterName, String docrootName, String indexName)
            throws IOException, URISyntaxException {
        super(globals);
        this.prefix = prefix == null ? scm + "/" : prefix;
        this.scm = scm;
        this.cluster = globals.network().get(clusterName);
        this.docrootName = docrootName;
        this.indexName = indexName.isEmpty() ? scm + ".idx" : indexName;
    }

    public void run() throws IOException, URISyntaxException {
        Docroot docroot;
        Filter filter;
        URI url;
        String scmurlstr;
        ScmProperties moduleConfig;
        Module module;
        Distributor distributor;
        long changed;
        Index index;
        FileNode cacheroot;
        HostProperties properties;

        properties = globals.properties();
        docroot = cluster.docroot(docrootName);
        filter = new Filter();
        filter.includeAll();
        url = properties.lookupScm(scm);
        if (url == null) {
            throw new IOException("scm not found is host properties: " + scm);
        }
        scmurlstr = "scm:" + url;
        moduleConfig = new ScmProperties(ScmProperties.urlToFilename(scmurlstr), false, filter, scmurlstr,
                "", "", false, "", prefix, null, null);
        cacheroot = globals.cacheroot();
        module = moduleConfig.create(cacheroot, true, properties.secrets, null);
        try (Pool pool = globals.pool()) {
            distributor = Distributor.open(cacheroot, cluster.connect(pool), docroot, indexName);
            changed = distributor.publish(module);
            index = distributor.close();
        }
        console.info.println("done: " + changed + "/" + index.size() + " files changed");
    }
}
