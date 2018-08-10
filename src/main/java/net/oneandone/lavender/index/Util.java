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
package net.oneandone.lavender.index;

import net.oneandone.sushi.fs.MkfileException;
import net.oneandone.sushi.fs.file.FileNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static synchronized byte[] md5(byte ... data) {
        return md5(data, data.length);
    }

    public static synchronized byte[] md5(byte[] data, int count) {
        DIGEST.update(data, 0, count);
        return DIGEST.digest();
    }

    //--


    private static int tmpNo = 1;

    // TODO: the normal tmp file mechanism is allowed to create files with rw- --- --- permission - which is a problem here!
    public static FileNode newTmpFile(FileNode parent) {
        FileNode file;

        while (true) {
            file = parent.join("_tmp_" + tmpNo);
            try {
                file.mkfile();
                file.getWorld().onShutdown().deleteAtExit(file);
                return file;
            } catch (MkfileException e) {
                // continue
                tmpNo++;
            }
        }
    }
}
