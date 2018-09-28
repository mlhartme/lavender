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
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Network;
import net.oneandone.lavender.config.HostProperties;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.util.Separator;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LavenderIT {
    /**
     * Test war file extraction from war and nested jar to two local CDNs
     */
    @Test
    public void war() throws Exception {
        String war;

        war = ClassLoader.getSystemResource("app-example-1.0.0.war").getFile();
        check("warFile", war,
                "32a/c032a1c5047a1cc42e19ee58c0c47/app-example/vi-ui.png:32ac032a1c5047a1cc42e19ee58c0c47\n" +
                "fa5/4e8a567edb3216a6d4133712a2c00/jar-module/vi-oneandone-presenter.png:fa54e8a567edb3216a6d4133712a2c00");
    }

    private static final String INDEX_NAME = "indexfilefortests.idx";

    public void check(String name, String warFile, String expected) throws Exception {
        boolean withSsh = false;
        Globals globals;
        HostProperties properties;
        World world;
        Node src;
        FileNode target;
        FileNode testhosts;
        FileNode war;
        long started;
        Network net;

        world = World.create(withSsh);
        target = world.guessProjectHome(getClass()).join("target");
        properties = HostProperties.load(world.guessProjectHome(getClass()).join("src/test/config/host.properties"), withSsh);
        properties.initTemp(target.join("ittemp"));
        System.out.println(name + " started: ");
        src = world.file(warFile);
        war = world.getTemp().createTempFile();
        src.copyFile(war);
        testhosts = target.join("it", name).deleteTreeOpt().mkdirs();
        net = net(testhosts);
        started = System.currentTimeMillis();
        globals = new Globals(world, Console.create(), new Main.Commandline("lavenderit"), "nouser", false, 600, properties, net);
        assertEquals(0, Main.doMain(globals, "-e", "war", war.getAbsolute(), "test", "web", INDEX_NAME));
        System.out.println(name + " done: " + (System.currentTimeMillis() - started) + " ms");
        for (FileNode host : testhosts.list()) {
            assertEquals(expected, entries(host.join("htdocs")));
        }
        // tmp space on pearls is very restricted
        war.deleteFile();
    }

    private Network net(FileNode testhosts) throws IOException {
        Network net;

        testhosts.deleteTreeOpt();
        testhosts.mkdir();
        net = new Network();
        net.add(new Cluster("test")
                .addLocalhost(testhosts.join("cdn1"))
                .addLocalhost(testhosts.join("cdn2"))
                .addDocroot("web", Arrays.asList( "fix1.uicdn.net", "fix2.uicdn.net", "fix3.uicdn.net", "fix4.uicdn.net"),
                        "htdocs", "indexes"));
        return net;
    }

    private String entries(Node<?> directory) throws IOException {
        World world;
        Filter filter;
        List<String> entries;
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
            entries.add(file.getRelative(directory) + ':' + md5);
        }
        Collections.sort(entries); // because it runs on different filesystems
        return Separator.RAW_LINE.join(entries);
    }
}
