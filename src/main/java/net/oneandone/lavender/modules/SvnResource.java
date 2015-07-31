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
package net.oneandone.lavender.modules;

import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.svn.SvnNode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.IOException;
import java.io.OutputStream;

public class SvnResource extends Resource {
    private final SvnModule module;

    private final SvnEntry entry;
    /**
     * revision this resource was last modified; not that copying a resource is not a modification.
     * Thus, the path pointing for this revision might differ from the current path. (This is usually happens if the resource is
     * tagged as part of the release process.
     */
    private final long lastModifiedRevision;

    /** revision this resource was seen in svn */
    private final long accessRevision;
    private final String path;
    private final int length;
    private final long lastModified;

    public SvnResource(SvnModule module, SvnEntry entry, long lastModifiedRevision, long accessRevision, String path, int length, long lastModified, SvnNode dataNode, byte[] lazyMd5) {
        this.module = module;
        this.entry = entry;
        this.lastModifiedRevision = lastModifiedRevision;
        this.accessRevision = accessRevision;
        this.path = path;
        this.length = length;
        this.lastModified = lastModified;

        this.dataNode = dataNode;
        this.dataBytes = null;

        this.lazyMd5 = lazyMd5;
    }

    @Override
    public byte[] getMd5() throws IOException {
        if (lazyMd5 == null) {
            lazyMd5 = md5(getData());
            entry.md5 = lazyMd5;
        }
        return lazyMd5;
    }

    // dataNode xor dataBytes is null
    private SvnNode dataNode;
    private byte[] dataBytes;

    protected byte[] lazyMd5;

    public String getPath() {
        return path;
    }

    public long getLastModified() throws IOException {
        return lastModified;
    }

    public boolean isOutdated() {
        try {
            return lastModified == dataNode.getLastModified();
        } catch (GetLastModifiedException e) {
            // not found
            return true;
        }
    }

    public String getOrigin() {
        return module.uri() + "/" + path;
    }

    public byte[] getData() throws IOException {
        SVNRepository repository;
        long loaded;

        if (dataBytes == null) {
            dataBytes = new byte[length];
            try (OutputStream dest = new FillOutputStream(dataBytes)) {
                repository = dataNode.getRoot().getRepository();
                try {
                    loaded = repository.getFile(dataNode.getPath(), accessRevision, null, dest);
                } catch (SVNException e) {
                    throw new IOException("svn failure: " + e.getMessage(), e);
                }
                if (loaded != accessRevision) {
                    throw new IllegalStateException(loaded + " " + accessRevision);
                }
            }
            dataNode = null;
        }
        return dataBytes;
    }

    public static class FillOutputStream extends OutputStream {
        private final byte[] dest;
        private int pos;

        public FillOutputStream(byte[] dest) {
            this.dest = dest;
            this.pos = 0;
        }

        @Override
        public void write(int b) throws IOException {
            dest[pos++] = (byte) b;
        }

        @Override
        public void write(byte bytes[], int ofs, int len) throws IOException {
            System.arraycopy(bytes, ofs, dest, pos, len);
            pos += len;
        }

        @Override
        public void close() {
            if (pos != dest.length) {
                throw new IllegalStateException(pos + " vs " + dest.length);
            }
        }
    }
}
