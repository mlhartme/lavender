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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.IOException;
import java.io.OutputStream;

public class SvnResource extends Resource {
    private final SvnModule module;

    private final String resourcePath;

    private final SvnEntry entry;

    private final long accessRevision;

    public SvnResource(SvnModule module, String resourcePath, SvnEntry entry, long accessRevision) {
        this.module = module;
        this.resourcePath = resourcePath;
        this.entry = entry;
        this.accessRevision = accessRevision;
    }

    public String getPath() {
        return resourcePath;
    }

    public String getContentId() {
        return Long.toString(entry.revision);
    }

    public boolean isOutdated() {
        try {
            return entry.revision == module.getRoot().join(entry.accessPath).getLatestRevision();
        } catch (SVNException e) {
            // not found
            return true;
        }
    }

    public String getOrigin() {
        return module.uri() + "/" + resourcePath;
    }

    public void writeTo(OutputStream dest) throws IOException {
        SVNRepository repository;
        long loaded;

        repository = module.getRoot().getRoot().getRepository();
        try {
            loaded = repository.getFile(module.getRoot().join(entry.accessPath).getPath(), accessRevision, null, dest);
        } catch (SVNException e) {
            throw new IOException("svn failure: " + e.getMessage(), e);
        }
        if (loaded != accessRevision) {
            throw new IllegalStateException(loaded + " " + accessRevision);
        }
    }
}
