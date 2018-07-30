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

import net.oneandone.inline.Console;
import net.oneandone.lavender.config.Alias;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Properties;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LavenderIT {
    /**
     * Test war file extraction from war and nested jar to two local CDNs
     */
    @Test
    public void war() throws Exception {
        String warFile = ClassLoader.getSystemResource("app-example-1.0.0.war").getFile();
        check("warFile", warFile, "39c34e3d6170d1cbc49eb625f18f4825");
    }

    private static final String INDEX_NAME = "indexfilefortests.idx";

    public void check(String name, String warFile, String expected) throws Exception {
        Globals globals;
        Properties properties;
        World world;
        Node src;
        FileNode target;
        FileNode testhosts;
        FileNode war;
        long started;
        Net net;

        boolean withSsh = false;
        world = World.create(withSsh);
        properties = Properties.load(world.file("test.properties.sample"), withSsh);
        System.out.println(name + " started: ");
        src = world.file(warFile);
        war = world.getTemp().createTempFile();
        src.copyFile(war);
        target = world.guessProjectHome(getClass()).join("target");
        testhosts = target.join("it", name);
        testhosts.deleteTreeOpt();
        testhosts.mkdirs();
        started = System.currentTimeMillis();
        net = net(testhosts);
        properties.initTemp(target.join("ittemp"));
        globals = new Globals(world, Console.create(), new Main.Commandline("lavenderit"), false, "nouser", false, 600, properties, net);
        assertEquals(0, Main.doMain(globals, "-e", "war", war.getAbsolute(), INDEX_NAME, "web=test"));
        System.out.println(name + " done: " + (System.currentTimeMillis() - started) + " ms");
        for (FileNode host : testhosts.list()) {
            assertEquals(expected, md5(host.join("htdocs")));
        }
        // tmp space on pearls is very restricted
        war.deleteFile();
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

    private String md5(Node<?> directory) throws IOException {
        World world;
        Filter filter;
        List<String> entries;
        Node tmp;
        String md5;

        world = directory.getWorld();
        filter = world.filter();
        filter.include("**/*");
        filter.predicate(Predicate.FILE);
        entries = new ArrayList<>();
        for (Node<?> file : directory.find(filter)) {
            if (file.getName().startsWith(INDEX_NAME)) {
                // because different machine sort entries differently. And there's a timestamp comment in the beginning
                md5 = Integer.toHexString(file.readProperties().hashCode());
            } else {
                md5 = file.md5();
            }
            entries.add(file.getRelative(directory) + ':' + md5 + '\n');
        }
        Collections.sort(entries); // because it runs on different filesystems
        tmp = world.memoryNode();
        return tmp.writeLines(entries).md5();
    }
}
