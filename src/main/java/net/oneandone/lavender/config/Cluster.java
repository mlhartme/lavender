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

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Type
public class Cluster {
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


    public Host host(String hostName) {
        for (Host host : hosts) {
            if (hostName.equals(host.getName())) {
                return host;
            }
        }
        throw new IllegalArgumentException("host not found: " + hostName);
    }

    public Docroot docroot(String docrootName) {
        for (Docroot docroot : docroots) {
            if (docrootName.equals(docroot.getName())) {
                return docroot;
            }
        }
        throw new IllegalArgumentException("docroot not found: " + docrootName);
    }

    public Cluster addLocalhost(FileNode basedir) throws IOException {
        basedir.mkdir();
        basedir.join("tmp").mkdir();
        basedir.join("indexes").mkdir();
        basedir.join("htdocs").mkdir();
        hosts.add(Host.localhost(basedir));
        return this;
    }

    public Cluster addDocroot(String type, List<String> domains, String docroot, String indexes) {
        docroots.add(new Docroot(type, domains, docroot, indexes));
        return this;
    }

    public List<Connection> connect(Pool pool) throws IOException {
        List<Connection> result;

        result = new ArrayList<>();
        for (Host host : hosts) {
            try {
                result.add(pool.connect(host));
            } catch (IOException e) {
                for (Connection connection : result) {
                    try {
                        connection.close();
                    } catch (IOException suppressed) {
                        e.addSuppressed(suppressed);
                    }
                }
                throw e;
            }
        }
        return result;
    }
}
