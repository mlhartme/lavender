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

import java.io.IOException;

public class BitbucketEntry {
    public final String publicPath;
    public final String accessPath;
    private final BitbucketContentMap contentMap;
    private String lazyContentId;

    public BitbucketEntry(String publicPath, String accessPath, BitbucketContentMap contentMap) {
        this.publicPath = publicPath;
        this.accessPath = accessPath;
        this.contentMap = contentMap;
        this.lazyContentId = null;
    }

    public String getContentId() throws IOException {
        if (lazyContentId == null) {
            lazyContentId = contentMap.lookup(accessPath);
        }
        return lazyContentId;
    }

    public String toString() {
        return publicPath + " " + accessPath + " " + lazyContentId;
    }

    public int hashCode() {
        return accessPath.hashCode();
    }

    public boolean equals(Object obj) {
        BitbucketEntry entry;

        if (obj instanceof BitbucketEntry) {
            entry = (BitbucketEntry) obj;
            return publicPath.equals(entry.publicPath) && accessPath.equals(entry.accessPath);
        }
        return false;
    }
}
