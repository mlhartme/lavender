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
package net.oneandone.lavender.publisher.config;

import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Cluster {
    public Host findHost(String name) {
        for (Host host : hosts) {
            if (name.equals(host.name)) {
                return host;
            }
        }
        return null;
    }

    //--

    public final List<Host> hosts;

    /** pointing to "fix" docroot */
    public final List<Docroot> docroots;

    public Cluster() {
        this.hosts = new ArrayList<>();
        this.docroots = new ArrayList<>();
    }

    public Cluster addCdn(String name) {
        String login = "wwwcdn";

        return addHost(name, login, "home/" + login + "/indexes");
    }

    public Cluster addStatint(String name) {
        String login = "wwwstatint";

        return addHost(name, login, "home/" + login + "/indexes");
    }

    public Cluster addFlash(String name) {
        return addHost(name, "flash",  ".lavender");
    }

    public Cluster addLocalhost(FileNode basedir, String index) throws IOException {
        basedir.mkdir();
        basedir.join(index).mkdirs();
        basedir.join("htdocs").mkdirOpt();
        hosts.add(Net.local(basedir.join(index)));
        return this;
    }


    public Cluster addHost(String name, String login, String indexbase) {
        hosts.add(new Host(name, login, indexbase));
        return this;
    }

    public Cluster addDocroot(String docroot, Alias ... aliases) {
        docroots.add(new Docroot(docroot, Arrays.asList(aliases)));
        return this;
    }

    public Object[] alias(String name) {
        for (Docroot docroot : docroots) {
            for (Alias alias : docroot.aliases) {
                if (name.equals(alias.name)) {
                    return new Object[] { docroot, alias };
                }
            }
        }
        throw new ArgumentException("alias not found: " + name);
    }
}
