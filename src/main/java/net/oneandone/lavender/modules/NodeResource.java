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
import java.io.OutputStream;

public class NodeResource extends Resource {
    public static NodeResource forBytes(World world, String path, byte... bytes) throws IOException {
        FileNode temp;

        temp = world.getTemp().createTempFile();
        temp.writeBytes(bytes);
        return forNode(temp, path);
    }

    public static NodeResource forNode(Node node, String path) throws IOException {
        return new NodeResource(node, path, node.getLastModified());
    }

    private final Node node;
    private final String path;
    private final long lastModified;

    private NodeResource(Node node, String path, long lastModified) {
        this.node = node;
        this.path = path;
        this.lastModified = lastModified;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getMd5Opt() {
        return null;
    }

    public String getContentId() {
        return Long.toString(lastModified);
    }

    public boolean isOutdated() {
        try {
            return node.getLastModified() != lastModified;
        } catch (GetLastModifiedException e) {
            // not found
            return true;
        }
    }

    public String getOrigin() {
        return node.getUri().toString();
    }

    public void writeTo(OutputStream dest) throws IOException {
        node.copyFileTo(dest);
    }
}
