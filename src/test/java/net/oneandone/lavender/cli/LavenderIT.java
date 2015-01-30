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
import net.oneandone.lavender.config.Settings;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Ignore
public class LavenderIT {
    @Test
    public void warOnly() throws Exception {
        check("war", "67f812dc689928f9d8ed4dc271143c62", "com/oneandone/sales/dslcancel-de/1.4.7/dslcancel-de-1.4.7.war");
    }

    @Test
    public void warWithDownloads() throws Exception {
        check("downloads", "1a70223661a8b527dd8f8867c360efd5", "de/ui/webdev/pfixui/1.1.46/pfixui-1.1.46.war");
    }

    @Test
    public void warWithFlashEu() throws Exception {
        check("flash-eu", "c955eefa4c272043fab7add008b74876",
                "com/oneandone/sales/diy/de/diy-business-de/2.1.147/diy-business-de-2.1.147.war");
    }

    private static final String INDEX_NAME = "indexfilefortests.idx";

    public void check(String name, String expected, String warUrl) throws Exception {
        Settings settings;
        World world;
        Node src;
        FileNode target;
        FileNode testhosts;
        FileNode war;
        FileNode warModified;
        long started;
        Net net;

        settings = Settings.load();
        world = settings.world;
        System.out.println(name + " started: ");
        src = world.node("http://mavenrepo.united.domain:8081/nexus/content/repositories/1und1-stable/" + warUrl);
        war = world.getTemp().createTempFile();
        warModified = world.getTemp().createTempFile();
        warModified.deleteFile();
        src.copyFile(war);
        target = world.guessProjectHome(getClass()).join("target");
        testhosts = target.join("it", name);
        testhosts.getParent().mkdirsOpt();
        started = System.currentTimeMillis();
        net = net(testhosts);
        settings.initTemp(target.join("ittemp"));
        assertEquals(0, Main.doMain(settings, net, "-e", "war", "test", war.getAbsolute(), warModified.getAbsolute(), INDEX_NAME));
        System.out.println(name + " done: " + (System.currentTimeMillis() - started) + " ms");

        // tmp space on pearls is very restricted
        war.deleteFile();
        warModified.deleteFile();

        assertEquals(expected, md5(testhosts));
    }

    private Net net(FileNode testhosts) throws IOException {
        Net net;

        testhosts.deleteTreeOpt();
        testhosts.mkdir();
        net = new Net();
        net.add(new Cluster("test")
                .addLocalhost(testhosts.join("cdn1"))
                .addLocalhost(testhosts.join("cdn2"))
                .addDocroot("web", "htdocs/fix", "indexes",
                        new Alias("fix", "/fix", "fix1.uicdn.net", "fix2.uicdn.net", "fix3.uicdn.net", "fix4.uicdn.net")));
        net.add(new Cluster("flash-eu")
                .addLocalhost(testhosts.join("flash-eu1"))
                .addLocalhost(testhosts.join("flash-eu2"))
                .addDocroot("flash", "htdocs", "htdocs/.lavender",
                        new Alias("main")));
        net.add(new Cluster("flash-us")
                .addLocalhost(testhosts.join("flash-us"))
                .addDocroot("flash", "htdocs", "htdocs/.lavender",
                        new Alias("main")));
        return net;
    }

    private String md5(Node directory) throws IOException {
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
        for (Node file : directory.find(filter)) {
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
