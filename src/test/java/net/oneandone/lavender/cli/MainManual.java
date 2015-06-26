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
package net.oneandone.lavender.cli;

import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Connection;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.Properties;
import net.oneandone.sushi.fs.Node;
import org.junit.Test;

public class MainManual {
    @Test
    public void sshAccess() throws Exception {
        Properties properties;
        Net net;
        Node node;

        properties = Properties.load();
        net = properties.loadNet();
        try (Pool pool = new Pool(properties.world, null, 0)) {
            for (Cluster cluster : net.clusters()) {
                for (Connection connection : cluster.connect(pool)) {
                    node = new Docroot("", "", "").node(connection);
                    System.out.println(connection.getHost() + ":\n  " + node.list());
                }
            }
        }
    }
}
