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
import net.oneandone.lavender.config.Pool;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** assumes that ssh localhost works for your machine without password */
public class MainManual {
    @Test
    public void sshAccess() throws Exception {
        World world;
        Node node;
        Cluster cluster;
        FileNode hostdir;
        FileNode testfile;
        List<? extends Node> lst;

        world = World.create(false);
        hostdir = world.getTemp().createTempDirectory().join("testcluster");
        cluster = new Cluster("test");
        cluster.addLocalhost(hostdir);
        cluster.addDocroot("web", Arrays.asList("no.such.domain"),"htdocs", "indexes");
        testfile = hostdir.join("htdocs").createTempFile();
        try (Pool pool = Pool.create(world, null, 0)) {
            for (Connection connection : cluster.connect(pool)) {
                node = cluster.docroot("web").node(connection);
                lst = node.list();
                assertEquals(1, lst.size());
                assertEquals(testfile.getName(), lst.get(0).getName());
            }
        }
    }
}
