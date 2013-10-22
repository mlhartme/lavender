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
package net.oneandone.lavender.config;

import net.oneandone.lavender.index.Distributor;

import java.io.IOException;

public class Target {
    public final Cluster cluster;
    public final Docroot docroot;
    public final Alias alias;

    public Target(Cluster cluster, Docroot docroot, Alias alias) {
        this.cluster = cluster;
        this.docroot = docroot;
        this.alias = alias;
    }

    public Distributor open(Pool pool, String indexName) throws IOException {
        return Distributor.open(cluster.connect(pool), docroot, indexName);
    }
}
