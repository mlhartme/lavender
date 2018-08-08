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

/** A line in the index file. Immutable. */
public class Label {
    private final String originalPath;
    private final String lavendelizedPath;
    private final byte[] hash;

    public Label(String originalPath, String lavendelizedPath, byte[] hash) {
        this.originalPath = originalPath;
        this.lavendelizedPath = lavendelizedPath;
        this.hash = hash;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getLavendelizedPath() {
        return lavendelizedPath;
    }

    public byte[] hash() {
        return hash;
    }

    public String toString() {
        return String.format("Label [originalPath=%s, lavendelizedPath=%s, md5=%s]", originalPath, lavendelizedPath, hash);
    }
}
