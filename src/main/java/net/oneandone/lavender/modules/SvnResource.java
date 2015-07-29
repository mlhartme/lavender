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

import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SvnResource extends Resource {
    private final SvnModule module;
    private final long revision;
    private final String path;
    private final int length;
    private final long lastModified;

    public SvnResource(SvnModule module, long revision, String path, int length, long lastModified, SvnNode dataNode, byte[] lazyMd5) {
        this.module = module;
        this.revision = revision;
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
            module.addIndex(new Label(Strings.removeLeft(getPath(), module.getResourcePathPrefix()), Long.toString(revision), lazyMd5));
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
        if (dataBytes == null) {
            dataBytes = new byte[length];
            try (OutputStream dest = new FixedOutputStream(dataBytes)) {
                dataNode.writeTo(dest);
            }
            dataNode = null;
        }
        return dataBytes;
    }

    public static class FixedOutputStream extends OutputStream {
        private final byte[] dest;
        private int pos;

        public FixedOutputStream(byte[] dest) {
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
