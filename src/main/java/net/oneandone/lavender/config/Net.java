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

import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.ComplexType;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.annotation.AnnotationSchema;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;

import java.util.ArrayList;
import java.util.List;

@Type
public class Net {
    public static final Schema SCHEMA = new AnnotationSchema();
    public static final ComplexType TYPE = SCHEMA.complex(Net.class);

    public static void main(String[] args) throws Exception {
        TYPE.instance(normal()).toXml(new World().file("net.xml"));
    }

    public static Net normal() {
        Net net;

        net = new Net();
        net.add(new Cluster("eu")
            .addCdn("cdnfe01.schlund.de")
            .addCdn("cdnfe02.schlund.de")
            .addCdn("cdnfe03.schlund.de")
            .addDocroot("web", "home/wwwcdn/htdocs/fix", "home/wwwcdn/indexes/fix",
                    new Alias("fix", "s1.uicdn.net", "s2.uicdn.net", "s3.uicdn.net", "s4.uicdn.net")));
        net.add(new Cluster("us")
                // see http://issue.tool.1and1.com/browse/ITOSHA-3624 and http://issue.tool.1and1.com/browse/ITOSHA-3667
                .addCdn("wscdnfelxaa01.fe.server.lan")
                .addCdn("wscdnfelxaa02.fe.server.lan")
                .addCdn("wscdnfelxaa03.fe.server.lan")
                .addDocroot("web", "home/wwwcdn/htdocs/fix", "home/wwwcdn/indexes/fix",
                        new Alias("fix", "u1.uicdn.net", "u2.uicdn.net", "u3.uicdn.net", "u4.uicdn.net"),
                        new Alias("akamai", "au1.uicdn.net", "au2.uicdn.net", "au3.uicdn.net", "au4.uicdn.net")));
        net.add(new Cluster("flash-eu")
                // see http://issue.tool.1and1.com/browse/ITOSHA-3624 and http://issue.tool.1and1.com/browse/ITOSHA-3668
                .addFlash("winflasheu1.schlund.de")
                .addFlash("winflasheu2.schlund.de")
                .addDocroot("flash", "", ".lavender",
                        new Alias("flash")));
        net.add(new Cluster("flash-us")
                .addFlash("winflashus1.lxa.perfora.net")
                .addFlash("winflashus2.lxa.perfora.net")
                .addDocroot("flash", "", ".lavender",
                        new Alias("flash")));

        // this cluster is only accessible from within 1&1
        net.add(new Cluster("internal")
                .addStatint("cdnfe01.schlund.de")
                .addStatint("cdnfe02.schlund.de")
                .addStatint("cdnfe03.schlund.de")
                /* the following is excluded to avoid garbage collection of the file inside:
                .addDocroot("var/bazaarvoice", "indexes/bazaarvoice",
                        new Alias("bazaar")) */
                .addDocroot("svn", "home/wwwstatint/htdocs/var/svn", "home/wwwstatint/indexes/svn",
                        new Alias("svn")));

        net.add(new Cluster("walter")
                .addHost("walter.websales.united.domain", "mhm")
                .addDocroot("web", "Users/mhm/lavender/htdocs/fix", "Users/mhm/lavender/indexes/fix",
                        new Alias("fix", "fix.lavender.walter.websales.united.domain"))
                .addDocroot("flash", "Users/mhm/lavender/htdocs/flash", "Users/mhm/lavender/indexes/flash",
                        new Alias("flash"))
                .addDocroot("svn", "Users/mhm/lavender/htdocs/var/svn", "Users/mhm/lavender/indexes/svn",
                        new Alias("svn")));

        return net;
    }

    //--

    @Sequence(Cluster.class)
    private final List<Cluster> clusters;

    public Net() {
        clusters = new ArrayList<>();
    }

    public List<Cluster> clusters() {
        return clusters;
    }

    public void add(Cluster cluster) {
        if (lookup(cluster.getName()) != null) {
            throw new IllegalArgumentException("duplicate cluster: " + cluster.getName());
        }
        clusters.add(cluster);
    }

    public Cluster lookup(String name) {
        for (Cluster cluster : clusters) {
            if (name.equals(cluster.getName())) {
                return cluster;
            }
        }
        return null;
    }

    public Cluster get(String cluster) {
        Cluster result;

        result = lookup(cluster);
        if (result == null) {
            throw new ArgumentException("no such cluster: " + cluster);
        }
        return result;
    }
}
