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

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DefaultResource extends Resource {
    public static DefaultResource forBytes(byte[] bytes, String path, String folder) {
        return new DefaultResource(URI.create("mem://" + path), path, bytes.length, System.currentTimeMillis(), folder, null, bytes, null);
    }

    public static DefaultResource forNode(Node node, String path, String folder) throws IOException {
        return new DefaultResource(node.getURI(), path, node.length(), node.getLastModified(), folder, node, null, null);
    }

    private final URI origin;
    private final String path;
    private final long length;
    private final long lastModified;
    private final String folder;

    // dataNode xor dataBytes is null
    private Node dataNode;
    private byte[] dataBytes;

    protected byte[] lazyMd5;

    public DefaultResource(URI origin, String path, long length, long lastModified, String folder,
                           Node dataNode, byte[] dataBytes, byte[] lazyMd5) {
        this.origin = origin;
        this.path = path;
        this.length = length;
        this.lastModified = lastModified;
        this.folder = folder;

        this.dataNode = dataNode;
        this.dataBytes = dataBytes;

        this.lazyMd5 = lazyMd5;
    }

    public long getSize() {
        return length;
    }

    public String getFolder() {
        return folder;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() throws IOException {
        return lastModified;
    }

    public URI getOrigin() {
        return origin;
    }

    public byte[] getMd5() throws IOException {
        if (lazyMd5 == null) {
            lazyMd5 = md5(getData());
        }
        return lazyMd5;
    }

    public byte[] getData() throws IOException {
        if (dataBytes == null) {
            dataBytes = dataNode.readBytes();
            dataNode = null;
        }
        return dataBytes;
    }
}
