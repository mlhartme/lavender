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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.config.Connection;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.index.Util;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Receives extracted files and uploads them */
public class Distributor {
    private static final Logger LOG = LoggerFactory.getLogger(Distributor.class);

    public static Distributor open(List<Connection> connections, Docroot docroot, String indexName) throws IOException {
        Node destroot;
        Node file;
        Map<Node, Node> targets;
        Index all;
        Index prev;

        targets = new LinkedHashMap<>(); // to preserve order
        if (connections.isEmpty()) {
            prev = new Index();
            all = new Index();
        } else {
            all = null;
            prev = null;
            for (Connection connection : connections) {
                destroot = docroot.node(connection);
                file = docroot.index(connection, indexName);
                prev = loadSame(file, prev);
                targets.put(file, destroot);
                all = loadSame(docroot.index(connection, Index.ALL_IDX), all);
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
                throw new IOException("index mismatch: " + src.getUri());
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

    /** @return number of changed (updated or added) resources */
    public long publish(World world, Module<?> module) throws IOException {
        String path;
        String contentId;
        Label label;
        long count;
        String name;
        Md5Cache cache;
        byte[] md5;
        byte[] data;
        boolean cacheModified;

        count = 0;
        name = module.getName();
        cache = Md5Cache.loadOrCreate(world, name);
        cacheModified = false;
        for (Resource resource : module) {
            path = resource.getPath();
            contentId = resource.getContentId();
            md5 = cache.lookup(path, contentId);
            if (md5 == null) {
                data = resource.getData();
                md5 = Util.md5(data);
                cache.add(path, contentId, md5);
                cacheModified = true;
            } else {
                data = null;
            }
            label = module.createLabel(resource, md5);
            if (write(label, resource, data)) {
                count++;
            }
        }
        if (cacheModified) {
            cache.save();
        }
        return count;
    }

    public boolean write(Label label, Resource resource, byte[] data) throws IOException {
        Node dest;
        String destPath;
        Label allLabel;
        boolean changed;
        Node tmp;

        destPath = label.getLavendelizedPath();
        allLabel = all.lookup(destPath);
        if (allLabel != null && Arrays.equals(allLabel.md5(), label.md5())) {
            changed = false;
        } else {
            if (data == null) {
                data = resource.getData();
            }
            if (LOG.isDebugEnabled()) {
                if (allLabel == null) {
                    LOG.debug("A " + destPath);
                } else {
                    LOG.debug("U " + destPath);
                }
            }
            for (Node destroot : targets.values()) {
                dest = destroot.join(destPath);
                if (allLabel == null) {
                    dest.getParent().mkdirsOpt();
                    dest.writeBytes(data);
                } else {
                    tmp = dest.getParent().join(".atomicUpdate");
                    tmp.writeBytes(data);
                    tmp.move(dest, true);
                }
            }
            changed = true;
        }
        next.add(label);
        return changed;
    }


    /** Writes modified indexes; return next index */
    public Index close() throws IOException {
        Node directory;
        Node index;

        for (Label label : prev) {
            if (!all.removeReferenceOpt(label.getLavendelizedPath())) {
                throw new IOException("not found in all.idx: " + label.getLavendelizedPath());
            }
        }
        for (Label label : next) {
            all.addReference(label.getLavendelizedPath(), label.md5());
        }
        for (Map.Entry<Node, Node> entry : targets.entrySet()) {
            index = entry.getKey();
            directory = index.getParent();
            directory.mkdirsOpt();
            next.save(index);
            all.save(directory.join(Index.ALL_IDX));
        }
        return next;
    }
}
