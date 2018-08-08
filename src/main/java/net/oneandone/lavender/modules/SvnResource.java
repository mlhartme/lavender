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

import net.oneandone.sushi.fs.GetLastModifiedException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.IOException;
import java.io.OutputStream;

public class SvnResource extends Resource {
    private final SvnModule module;

    private final String resourcePath;

    private final SvnEntry entry;

    private final long accessRevision;

    private byte[] lazyData;

    public SvnResource(SvnModule module, String resourcePath, SvnEntry entry, long accessRevision) {
        this.module = module;
        this.resourcePath = resourcePath;
        this.entry = entry;
        this.accessRevision = accessRevision;
        this.lazyData = null;
    }

    @Override
    public byte[] getHash() throws IOException {
        if (entry.md5 == null) {
            entry.md5 = md5(getData());
        }
        return entry.md5;
    }

    public String getPath() {
        return resourcePath;
    }

    public String getContentId() {
        return Long.toString(entry.revision);
    }

    public boolean isOutdated() {
        try {
            return entry.time == module.getRoot().join(entry.accessPath).getLastModified();
        } catch (GetLastModifiedException e) {
            // not found
            return true;
        }
    }

    public String getOrigin() {
        return module.uri() + "/" + resourcePath;
    }

    public byte[] getData() throws IOException {
        SVNRepository repository;
        long loaded;

        if (lazyData == null) {
            lazyData = new byte[entry.size];
            try (OutputStream dest = new FillOutputStream(lazyData)) {
                repository = module.getRoot().getRoot().getRepository();
                try {
                    loaded = repository.getFile(module.getRoot().join(entry.accessPath).getPath(), accessRevision, null, dest);
                } catch (SVNException e) {
                    throw new IOException("svn failure: " + e.getMessage(), e);
                }
                if (loaded != accessRevision) {
                    throw new IllegalStateException(loaded + " " + accessRevision);
                }
            }
        }
        return lazyData;
    }

    public static class FillOutputStream extends OutputStream {
        private final byte[] dest;
        private int pos;

        public FillOutputStream(byte[] dest) {
            this.dest = dest;
            this.pos = 0;
        }

        @Override
        public void write(int b) {
            dest[pos++] = (byte) b;
        }

        @Override
        public void write(byte[] bytes, int ofs, int len) {
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
