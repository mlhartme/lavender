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
package net.oneandone.lavender.config;

import net.oneandone.sushi.fs.World;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetworkTest {
    @Test
    public void load() throws IOException {
        World world;
        Properties properties;
        Network network;
        Cluster cluster;

        world = World.create(false);
        properties = Properties.load(world.guessProjectHome(Network.class).join("src/test/resources/lavender.properties"), false);
        network = properties.loadNetwork();
        cluster = network.lookup("localhost");
        assertNotNull(cluster);
        assertEquals("localhost", cluster.getName());
        assertNotNull(cluster.docroot("web"));
    }
}
