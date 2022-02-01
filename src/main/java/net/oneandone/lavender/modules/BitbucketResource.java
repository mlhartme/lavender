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

import net.oneandone.lavender.scm.Bitbucket;

import java.io.IOException;
import java.io.OutputStream;

public class BitbucketResource extends Resource {
    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final BitbucketEntry entry;
    private final String at;

    public BitbucketResource(Bitbucket bitbucket, String project, String repository, String resourcePath, BitbucketEntry entry, String at) {
        super(resourcePath);
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.entry = entry;
        this.at = at;
    }

    @Override
    public String getMd5Opt() {
        return null;
    }

    @Override
    public String getContentId() {
        try {
            return entry.getContentId();
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
    }

    @Override
    public String getOrigin() {
        return "bitbucket:" + project + ":" + repository + ":" + entry + ":" + at;
    }

    @Override
    public void writeTo(OutputStream dest) throws IOException {
        bitbucket.writeTo(project, repository, entry.accessPath, at, dest);
    }

    @Override
    public boolean isOutdated() {
        // the least expensive way I know to check for changes is to re-load changes with content ids
        // (note that Bitbucket's lastModified api call didn't work for me, some command wasn'd found on the server)
        return true;
    }
}
