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

public class BitbucketEntry {
    public final String publicPath;
    public final String accessPath;
    public final String contentId;

    public BitbucketEntry(String publicPath, String accessPath, String contentId) {
        this.publicPath = publicPath;
        this.accessPath = accessPath;
        this.contentId = contentId;
    }

    public String toString() {
        return publicPath + " " + accessPath + " " + contentId;
    }

    public int hashCode() {
        return contentId.hashCode();
    }

    public boolean equals(Object obj) {
        BitbucketEntry entry;

        if (obj instanceof BitbucketEntry) {
            entry = (BitbucketEntry) obj;
            return publicPath.equals(entry.publicPath) && accessPath.equals(entry.accessPath) && contentId.equals(entry.contentId);
        }
        return false;
    }
}
