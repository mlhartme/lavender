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
package net.oneandone.lavender.index;

import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Host;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Receives extracted files and uploads them */
public class Distributor {
    public static final String ALL_IDX = ".all.idx";

    public static Distributor open(World world, List<Host> hosts, Docroot docroot, String indexName) throws IOException {
        Node root;
        Node destroot;
        Node file;
        Map<Node, Node> targets;
        Index all;
        Index prev;

        targets = new LinkedHashMap<>(); // to preserve order
        if (hosts.isEmpty()) {
            prev = new Index();
            all = new Index();
        } else {
            all = null;
            prev = null;
            for (Host host : hosts) {
                root = host.open(world);
                destroot = docroot.node(root);
                file = docroot.index(root, indexName);
                prev = loadSame(file, prev);
                targets.put(file, destroot);
                all = loadSame(docroot.index(root, ALL_IDX), all);
            }
        }
        return new Distributor(targets, all, prev);
    }

    private static Index loadSame(Node src, Index prev) throws IOException {
        Index tmp;

        if (src.exists()) {
            tmp = Index.load(src);
        } else {
            tmp = new Index();
        }
        if (prev == null) {
            return tmp;
        } else {
            if (!prev.equals(tmp)) {
                throw new IOException("index mismatch");
            }
            return prev;
        }
    }

    /** left: index location; right: docroot */
    private final Map<Node, Node> targets;
    private final Index all;
    private final Index prev;
    private final Index next;

    public Distributor(Map<Node, Node> targets, Index all, Index prev) {
        this.targets = targets;
        this.all = all;
        this.prev = prev;
        this.next = new Index();
    }

    public boolean write(Label label, byte[] data) throws IOException {
        Node dest;
        String destPath;
        Label allLabel;
        boolean changed;

        destPath = label.getLavendelizedPath();
        allLabel = all.lookup(destPath);
        if (allLabel != null && Arrays.equals(allLabel.md5(), label.md5())) {
            changed = false;
        } else {
            for (Node destroot : targets.values()) {
                dest = destroot.join(destPath);
                if (allLabel == null) {
                    dest.getParent().mkdirsOpt();
                }
                dest.writeBytes(data);
            }
            changed = true;
        }
        next.add(label);
        return changed;
    }

    /** return next index */
    public Index close() throws IOException {
        Node directory;
        Node index;

        for (Label label : prev) {
            all.removeReference(label.getLavendelizedPath());
        }
        for (Label label : next) {
            all.addReference(label.getLavendelizedPath(), label.md5());
        }
        for (Map.Entry<Node, Node> entry : targets.entrySet()) {
            index = entry.getKey();
            directory = index.getParent();
            directory.mkdirsOpt();
            next.save(index);
            all.save(directory.join(ALL_IDX));
        }
        return next;
    }
}
