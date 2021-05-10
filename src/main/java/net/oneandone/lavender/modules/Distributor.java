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
import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.index.Util;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Receives extracted files and uploads them */
public class Distributor {
    private static final Logger LOG = LoggerFactory.getLogger(Distributor.class);

    public static Distributor open(FileNode cacheroot, List<Connection> connections, Docroot docroot, String indexName) throws IOException {
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
        return new Distributor(cacheroot, targets, all, prev);
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
    private final Buffer buffer;
    private final FileNode cacheroot;
    private final Map<Node, Node> targets;
    private final Index all;
    private final Index prev;
    private final Index next;

    public Distributor(FileNode cacheroot, Map<Node, Node> targets, Index all, Index prev) {
        this.buffer = new Buffer();
        this.cacheroot = cacheroot;
        this.targets = targets;
        this.all = all;
        this.prev = prev;
        this.next = new Index();
    }

    /** @return number of changed (updated or added) resources */
    public long publish(Module<?> module) throws IOException {
        FileNode cacheFile;
        String path;
        String contentId;
        Label label;
        long count;
        String md5str;
        byte[] md5;
        boolean dataBuffered;

        count = 0;
        // it's not save to base the file on the simple module name even though lookup always includes the content id --
        // different modules may have the same name (webapp!), and both of them may container different files at the same path with the
        // same contentId. This happend for "vi-presender-domain-new.png"
        cacheFile = cacheroot.join("md5", ModuleProperties.urlToFilename(module.getOrigin()) + ".cache");
        try (Md5Cache cache = Md5Cache.loadOrCreate(cacheFile)) {
            for (Resource resource : module) {
                buffer.reset();
                path = resource.getResourcePath();
                md5str = resource.getMd5Opt();
                if (md5str == null) {
                    contentId = resource.getContentId();
                    md5 = cache.lookup(path, contentId);
                    if (md5 == null) {
                        resource.writeTo(buffer);
                        dataBuffered = true;
                        md5 = buffer.md5();
                        cache.add(path, contentId, md5);
                    } else {
                        dataBuffered = false;
                    }
                } else {
                    md5 = Hex.decodeString(md5str);
                    dataBuffered = false;
                }
                label = module.createLabel(resource, md5);
                if (write(label, resource, dataBuffered)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** @return true if resource was written, false if it's already in the all index */
    public boolean write(Label label, Resource resource, boolean dataBuffered) throws IOException {
        Node dest;
        String destPath;
        Label allLabel;
        Node tmp;

        next.add(label);
        destPath = label.getLavendelizedPath();
        allLabel = all.lookup(destPath);
        if (allLabel != null && Arrays.equals(allLabel.md5(), label.md5())) {
            return false;
        }
        if (!dataBuffered) {
            resource.writeTo(buffer);
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
                try (OutputStream out = dest.newOutputStream()) {
                    buffer.writeTo(out);
                }
            } else {
                tmp = dest.getParent().join(".atomicUpdate"); // because apache is happily serving files while we update them ...
                try (OutputStream out = tmp.newOutputStream()) {
                    buffer.writeTo(out);
                }
                tmp.move(dest, true);
            }
        }
        return true;
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

    //--

    public static class Buffer extends ByteArrayOutputStream {
        private final int initialBytes;

        public Buffer() {
            this(512 * 1024);
        }

        public Buffer(int initialBytes) {
            super(initialBytes);

            this.initialBytes = initialBytes;
        }

        @Override
        public void reset() {
            super.reset();
            if (buf.length != initialBytes) {
                buf = new byte[initialBytes];
            }
        }

        public byte[] md5() {
            return Util.md5(buf, count);
        }
    }
}
