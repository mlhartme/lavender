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
import net.oneandone.lavender.config.Properties;
import net.oneandone.lavender.config.Target;
import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.SvnProperties;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Svn extends Base {
    private final String type;
    private final String directory;
    private final Cluster cluster;

    public Svn(Globals globals, String type, String directory, String clusterName) throws IOException {
        super(globals);
        this.type = type;
        this.directory = directory;
        this.cluster = globals.net().get(clusterName);
    }

    public void run() throws IOException {
        String svn;
        Docroot docroot;
        Target target;
        Filter filter;
        String svnurl;
        SvnProperties moduleConfig;
        Module module;
        Distributor distributor;
        long changed;
        Index index;
        FileNode cache;
        Properties properties;

        if (directory.isEmpty() || directory.contains("/")) {
            throw new ArgumentException("invalid directory: " + directory);
        }
        properties = globals.getProperties();
        svn = Strings.removeLeft(properties.svn.toString(), "svn:");
        docroot = cluster.docroot(type);
        target = new Target(cluster, docroot, docroot.aliases().get(0));
        filter = new Filter();
        filter.includeAll();
        svnurl = svn + "/data/" + directory;
        moduleConfig = new SvnProperties("svn", filter, svnurl, -1, svnurl, Docroot.WEB, false, "", directory + "/", null);
        cache = globals.lockedCache();
        try {
            module = moduleConfig.create(cache, true, properties.svnUsername, properties.svnPassword, null);
            try (Pool pool = globals.pool()) {
                distributor = target.open(pool, directory + ".idx");
                changed = module.publish(distributor);
                index = distributor.close();
                module.saveCaches();
            }
        } finally {
            properties.unlockCache();
        }
        console.info.println("done: " + changed + "/" + index.size() + " files changed");
    }
}
