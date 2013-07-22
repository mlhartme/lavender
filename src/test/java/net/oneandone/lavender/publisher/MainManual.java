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
package net.oneandone.lavender.publisher;

import net.oneandone.lavender.publisher.Main;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Host;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Settings;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import org.junit.Test;

public class MainManual {
    @Test
    public void sshAccess() throws Exception {
        Net net;
        World world;
        Node node;

        net = Net.normal();
        world = new World();
        Main.initWorld(world, Settings.load(world), null);
        for (Cluster cluster : net.clusters.values()) {
            for (Host host : cluster.hosts) {
                node = new Docroot("", "").node(host.open(world));
                System.out.println(host + ":\n  " + node.list());
            }
        }
    }
}
