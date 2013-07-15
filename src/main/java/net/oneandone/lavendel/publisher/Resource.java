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
package net.oneandone.lavendel.publisher;

import net.oneandone.lavendel.index.Hex;
import net.oneandone.lavendel.index.Label;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Resource {
    private final byte[] data;
    private final String path;
    private final String folderName;

    public Resource(byte[] data, String path, String folderName) {
        this.data = data;
        this.path = path;
        this.folderName = folderName;
    }

    public byte[] getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path + "[" + data.length + "]";
    }


    public Label labelLavendelized(String pathPrefix) {
        String filename;
        byte[] md5;
        String md5str;

        filename = path.substring(path.lastIndexOf('/') + 1); // ok when not found
        md5 = md5();
        md5str = Hex.encodeString(md5);
        if (md5str.length() < 3) {
            throw new IllegalArgumentException(md5str);
        }
        return new Label(path, pathPrefix + md5str.substring(0, 3) + "/" + md5str.substring(3) + "/" + folderName + "/" + filename, md5);
    }

    public Label labelNormal(String pathPrefix) {
        return new Label(path, pathPrefix + path, md5());
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

    public byte[] md5() {
        DIGEST.update(data, 0, data.length);
        return DIGEST.digest();
    }

}
