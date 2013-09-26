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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.config.Filter;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.svn.SvnNode;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Extracts resources from svn */
public class SvnModule extends Module {
    private final SvnNode root;
    private final Filter filter;

    // maps svn paths (relative to root) to revision numbers
    private final Index oldIndex;
    private final Index index;
    private final Node indexFile;

    private final String resourcePathPrefix;

    private Map<String, SVNDirEntry> files;

    public SvnModule(Filter filter, String type, Index oldIndex, Index index, Node indexFile, SvnNode root,
                     boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, String folder) {
        super(type, folder, lavendelize, targetPathPrefix);
        this.root = root;
        this.filter = filter;
        this.oldIndex = oldIndex;
        this.index = index;
        this.indexFile = indexFile;
        this.resourcePathPrefix = resourcePathPrefix;

        this.files = null;
    }

    public Iterator<Resource> iterator() {
        final Iterator<Map.Entry<String, SVNDirEntry>> base;

        scan();
        base = files.entrySet().iterator();
        return new Iterator<Resource>() {
            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public Resource next() {
                Map.Entry<String, SVNDirEntry> entry;
                Label label;
                byte[] md5;

                entry = base.next();
                label = oldIndex.lookup(entry.getValue().getRelativePath());
                if (label != null && label.getLavendelizedPath().equals(Long.toString(entry.getValue().getRevision()))) {
                    md5 = label.md5();
                    index.add(label);
                } else {
                    md5 = null;
                }
                return createResource(entry.getValue(), md5);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void scan() {
        if (files != null) {
            return;
        }

        files = new HashMap<>();
        try {
            root.getRoot().getClientMananger().getLogClient().doList(
                    root.getSvnurl(), null, SVNRevision.HEAD, true, SVNDepth.INFINITY, SVNDirEntry.DIRENT_ALL, new ISVNDirEntryHandler() {
                @Override
                public void handleDirEntry(SVNDirEntry entry) throws SVNException {
                    String path;

                    if (entry.getKind() == SVNNodeKind.FILE) {
                        path = entry.getRelativePath();
                        if (filter.isIncluded(path)) {
                            if (entry.getSize() > Integer.MAX_VALUE) {
                                throw new UnsupportedOperationException("file too big: " + path);
                            }
                            files.put(resourcePathPrefix + path, entry);
                        }
                    }
                }
            });
        } catch (SVNException e) {
            throw new RuntimeException("TODO", e);
        }
    }

    private SvnResource createResource(SVNDirEntry entry, byte[] md5) {
        String svnPath;

        svnPath = entry.getRelativePath();
        return new SvnResource(this, entry.getRevision(), resourcePathPrefix + svnPath,
                (int) entry.getSize(), entry.getDate().getTime(), root.join(svnPath), md5);
    }

    public SvnResource probe(String path) throws IOException {
        SVNDirEntry entry;

        scan();
        entry = files.get(path);
        return entry == null ? null : createResource(entry, null);
    }

    public Index index() {
        return index;
    }

    public String uri() {
        return root.getURI().toString();
    }

    public void saveCaches() throws IOException {
        index.save(indexFile);
    }


}
