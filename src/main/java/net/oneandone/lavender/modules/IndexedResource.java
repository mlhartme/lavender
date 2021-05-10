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
import java.io.OutputStream;

public class IndexedResource extends Resource {
    private final UrlPattern urlPattern;
    private final String accessPath;
    private final String md5;

    public IndexedResource(UrlPattern urlPattern, String resourcePath, String accessPath, String md5) {
        super(resourcePath);
        this.urlPattern = urlPattern;
        this.accessPath = accessPath;
        this.md5 = md5;
    }

    @Override
    public String getMd5Opt() {
        return md5;
    }

    @Override
    public String getContentId() {
        return md5;
    }

    @Override
    public String getOrigin() {
        return urlPattern.getOrigin();
    }

    @Override
    public void writeTo(OutputStream dest) throws IOException {
        urlPattern.writeTo(accessPath, dest);
    }

    @Override
    public boolean isOutdated() {
        return false; // indexed resources are never out-dated
    }
}
