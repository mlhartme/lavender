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

import net.oneandone.inline.ArgumentException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.metadata.ComplexType;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.annotation.AnnotationSchema;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Type
public class Network {
    private static final Schema SCHEMA = new AnnotationSchema();
    private static final ComplexType TYPE = SCHEMA.complex(Network.class);

    public static Network load(Node src) throws IOException {
        return (Network) Network.TYPE.loadXml(src).get();
    }

    //--

    @Sequence(Cluster.class)
    private final List<Cluster> clusters;

    public Network() {
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
