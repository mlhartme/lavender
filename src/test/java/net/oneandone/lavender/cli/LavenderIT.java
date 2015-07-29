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

import net.oneandone.lavender.config.Alias;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Properties;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LavenderIT {
    @Test
    public void war() throws Exception {
        // TODO
        check("war", "/Users/mhm/Downloads/dslcancel-de-1.5.9.war");
    }

    private static final String INDEX_NAME = "indexfilefortests.idx";

    public void check(String name, String warFile) throws Exception {
        Properties properties;
        World world;
        Node src;
        FileNode target;
        FileNode testhosts;
        FileNode war;
        FileNode warModified;
        long started;
        Net net;

        world = new World();
        properties = Properties.load(world.file("test.properties"), true);
        System.out.println(name + " started: ");
        src = world.file(warFile);
        war = world.getTemp().createTempFile();
        warModified = world.getTemp().createTempFile();
        warModified.deleteFile();
        src.copyFile(war);
        target = world.guessProjectHome(getClass()).join("target");
        testhosts = target.join("it", name);
        testhosts.deleteTreeOpt();
        testhosts.mkdirs();
        started = System.currentTimeMillis();
        net = net(testhosts);
        properties.initTemp(target.join("ittemp"));
        assertEquals(0, Main.doMain(properties, net, "-e", "war", war.getAbsolute(), warModified.getAbsolute(), INDEX_NAME, "web=test"));
        System.out.println(name + " done: " + (System.currentTimeMillis() - started) + " ms");

        // tmp space on pearls is very restricted
        war.deleteFile();
        warModified.deleteFile();
    }

    private Net net(FileNode testhosts) throws IOException {
        Net net;

        testhosts.deleteTreeOpt();
        testhosts.mkdir();
        net = new Net();
        net.add(new Cluster("test")
                .addLocalhost(testhosts.join("cdn1"))
                .addLocalhost(testhosts.join("cdn2"))
                .addDocroot("web", "htdocs", "indexes",
                        new Alias("fix", "fix1.uicdn.net", "fix2.uicdn.net", "fix3.uicdn.net", "fix4.uicdn.net")));
        return net;
    }
}
