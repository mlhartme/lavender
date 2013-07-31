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

import net.oneandone.sushi.fs.LengthException;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Resource {
    private final Node node;
    private final String path;
    private final String folder;

    public Resource(Node node, String path, String folder) {
        this.node = node;
        this.path = path;
        this.folder = folder;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() throws IOException {
        return node.getLastModified();
    }

    public URI getOrigin() {
        return node.getURI();
    }

    public byte[] readData() throws IOException {
        return node.readBytes();
    }

    @Override
    public String toString() {
        String length;

        try {
            length = Long.toString(node.length());
        } catch (LengthException e) {
            length = "?";
        }
        return path + "[" + length + "] ->" + node.getURI();
    }


    public Label labelLavendelized(String pathPrefix, byte[] md5) {
        String filename;
        String md5str;

        filename = path.substring(path.lastIndexOf('/') + 1); // ok when not found
        md5str = Hex.encodeString(md5);
        if (md5str.length() < 3) {
            throw new IllegalArgumentException(md5str);
        }
        return new Label(path, pathPrefix + md5str.substring(0, 3) + "/" + md5str.substring(3) + "/" + folder + "/" + filename, md5);
    }

    public Label labelNormal(String pathPrefix, byte[] md5) {
        return new Label(path, pathPrefix + path, md5);
    }

    //-- utils

    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static byte[] md5(byte ... data) {
        DIGEST.update(data, 0, data.length);
        return DIGEST.digest();
    }

}
