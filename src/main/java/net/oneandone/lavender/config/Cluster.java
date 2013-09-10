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

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Type
public class Cluster {
    public Host findHost(String name) {
        for (Host host : hosts) {
            if (name.equals(host.getName())) {
                return host;
            }
        }
        return null;
    }

    //--

    @Value
    private String name;

    @Sequence(Host.class)
    private final List<Host> hosts;

    @Sequence(Docroot.class)
    private final List<Docroot> docroots;

    public Cluster() {
        this("dummy");
    }

    public Cluster(String name) {
        this.name = name;
        this.hosts = new ArrayList<>();
        this.docroots = new ArrayList<>();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Host> hosts() {
        return hosts;
    }

    public List<Docroot> docroots() {
        return docroots;
    }


    public Docroot docroot(String type) {
        for (Docroot docroot : docroots) {
            if (type.equals(docroot.getType())) {
                return docroot;
            }
        }
        throw new IllegalArgumentException("no docroot for type " + type);
    }

    public Cluster addCdn(String name) {
        return addHost(name, "wwwcdn");
    }

    public Cluster addStatint(String name) {
        return addHost(name, "wwwstatint");
    }

    public Cluster addFlash(String name) {
        return addHost(name, "flash");
    }

    public Cluster addLocalhost(FileNode basedir) throws IOException {
        basedir.mkdir();
        basedir.join("indexes").mkdirs();
        basedir.join("htdocs").mkdirOpt();
        hosts.add(Host.local(basedir));
        return this;
    }


    public Cluster addHost(String name, String login) {
        hosts.add(Host.remote(name, login));
        return this;
    }

    public Cluster addDocroot(String type, String docroot, String indexes, Alias... aliases) {
        docroots.add(new Docroot(type, docroot, indexes, Arrays.asList(aliases)));
        return this;
    }
}
