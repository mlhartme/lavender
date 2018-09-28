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
import net.oneandone.lavender.config.SystemProperties;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Distributor;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.ScmProperties;
import net.oneandone.lavender.modules.Secrets;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URISyntaxException;

public class Svn extends Base {
    private final String directory;
    private final Cluster cluster;
    private final String docrootName;

    public Svn(Globals globals, String directory, String clusterName, String docrootName) throws IOException, URISyntaxException {
        super(globals);
        this.directory = directory;
        this.cluster = globals.network().get(clusterName);
        this.docrootName = docrootName;
    }

    public void run() throws IOException, URISyntaxException {
        Docroot docroot;
        Filter filter;
        String scmurl;
        ScmProperties moduleConfig;
        Module module;
        Distributor distributor;
        long changed;
        Index index;
        FileNode cacheroot;
        SystemProperties properties;
        Secrets.UsernamePassword up;

        if (directory.isEmpty() || directory.contains("/")) {
            throw new ArgumentException("invalid directory: " + directory);
        }
        properties = globals.properties();
        docroot = cluster.docroot(docrootName);
        filter = new Filter();
        filter.includeAll();
        scmurl = "scm:" + properties.svn.toString() + "/data/" + directory;
        moduleConfig = new ScmProperties("svn", filter, scmurl, scmurl, "-1", "", Module.TYPE, false, "", directory + "/", null);
        cacheroot = globals.cacheroot();
        up = properties.secrets.get(scmurl);
        module = moduleConfig.create(cacheroot, true, up, null);
        try (Pool pool = globals.pool()) {
            distributor = Distributor.open(cacheroot, cluster.connect(pool), docroot, directory + ".idx");
            changed = distributor.publish(module);
            index = distributor.close();
        }
        console.info.println("done: " + changed + "/" + index.size() + " files changed");
    }
}
