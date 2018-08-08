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
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class DefaultResource extends Resource {
    public static DefaultResource forBytes(World world, byte[] bytes, String path) throws IOException {
        FileNode temp;

        temp = world.getTemp().createTempFile();
        temp.writeBytes(bytes);
        return forNode(temp, path);
    }

    public static DefaultResource forNode(Node node, String path) throws IOException {
        return new DefaultResource(node.getUri().toString(), path, node.getLastModified(), node,null);
    }

    private final String origin;
    private final String path;
    private final long lastModified;

    private Node dataNode;

    private byte[] lazyBytes;
    protected byte[] lazyMd5;

    private DefaultResource(String origin, String path, long lastModified, Node dataNode, byte[] lazyMd5) {
        this.origin = origin;
        this.path = path;
        this.lastModified = lastModified;

        this.dataNode = dataNode;

        this.lazyBytes = null;
        this.lazyMd5 = lazyMd5;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isOutdated() {
        try {
            return dataNode.getLastModified() != lastModified;
        } catch (GetLastModifiedException e) {
            // not found
            return true;
        }
    }

    public String getOrigin() {
        return origin;
    }

    public byte[] getMd5() throws IOException {
        if (lazyMd5 == null) {
            lazyMd5 = md5(getData());
        }
        return lazyMd5;
    }

    public byte[] getData() throws IOException {
        if (lazyBytes == null) {
            lazyBytes = dataNode.readBytes();
            dataNode = null;
        }
        return lazyBytes;
    }
}
