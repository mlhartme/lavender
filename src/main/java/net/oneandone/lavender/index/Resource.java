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
    private final Node origin;
    private final String path;
    private final String folder;

    private byte[] data;
    private byte[] md5;

    public Resource(Node origin, String path, String folder) {
        this.origin = origin;
        this.path = path;
        this.folder = folder;

        this.data = null;
        this.md5 = null;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() throws IOException {
        return origin.getLastModified();
    }

    public URI getOrigin() {
        return origin.getURI();
    }

    public byte[] getMd5() throws IOException {
        if (md5 == null) {
            md5 = md5(getData());
        }
        return md5;
    }

    public byte[] getData() throws IOException {
        if (data == null) {
            data = origin.readBytes();
        }
        return data;
    }

    @Override
    public String toString() {
        String length;

        try {
            length = Long.toString(origin.length());
        } catch (LengthException e) {
            length = "?";
        }
        return path + "[" + length + "] ->" + origin.getURI();
    }


    public Label labelLavendelized(String pathPrefix) throws IOException {

        String filename;
        String md5str;

        filename = path.substring(path.lastIndexOf('/') + 1); // ok when not found
        md5str = Hex.encodeString(getMd5());
        if (md5str.length() < 3) {
            throw new IllegalArgumentException(md5str);
        }
        return new Label(path, pathPrefix + md5str.substring(0, 3) + "/" + md5str.substring(3) + "/" + folder + "/" + filename, getMd5());
    }

    public Label labelNormal(String pathPrefix) throws IOException {
        return new Label(path, pathPrefix + path, getMd5());
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
