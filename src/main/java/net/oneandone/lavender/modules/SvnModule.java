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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Extracts resources from svn */
public class SvnModule extends Module {
    private final SvnNode root;

    private final Index oldIndex;
    private final Index index;
    private final Node indexFile;

    private final String resourcePathPrefix;

    public SvnModule(Filter filter, String type, Index oldIndex, Index index, Node indexFile, SvnNode root,
                     boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, String folder) {
        super(filter, type, folder, lavendelize, targetPathPrefix);
        this.root = root;
        this.oldIndex = oldIndex;
        this.index = index;
        this.indexFile = indexFile;
        this.resourcePathPrefix = resourcePathPrefix;
    }

    public Iterator<Resource> iterator() {
        final Iterator<SVNDirEntry> base;
        final List<SVNDirEntry> entries;

        entries = new ArrayList<>();
        try {
            root.getRoot().getClientMananger().getLogClient().doList(
                    root.getSvnurl(), null, SVNRevision.HEAD, true, SVNDepth.INFINITY, SVNDirEntry.DIRENT_ALL, new ISVNDirEntryHandler() {
                @Override
                public void handleDirEntry(SVNDirEntry entry) throws SVNException {
                    if (entry.getKind() == SVNNodeKind.FILE) {
                        entries.add(entry);
                    }
                }
            });
        } catch (SVNException e) {
            throw new RuntimeException("TODO", e);
        }
        base = entries.iterator();
        return new Iterator<Resource>() {
            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public Resource next() {
                SVNDirEntry entry;
                String path;
                Label label;
                byte[] md5;

                entry = base.next();
                path = entry.getRelativePath();
                label = oldIndex.lookup(path);
                if (label != null && label.getLavendelizedPath().equals(Long.toString(entry.getRevision()))) {
                    md5 = label.md5();
                    index.add(label);
                } else {
                    md5 = null;
                }
                return createResource(entry, path, md5);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** @param path relative to root */
    private SvnResource createResource(SVNDirEntry entry, String path, byte[] md5) {
        if (entry.getSize() > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("file too big: " + path);
        }
        return new SvnResource(this, entry.getRevision(), resourcePathPrefix + path,
                (int) entry.getSize(), entry.getDate().getTime(), root.join(path), md5);
    }

    public SvnResource probeIncluded(String path) throws IOException {
        SVNDirEntry entry;

        if (!path.startsWith(resourcePathPrefix)) {
            return null;
        }
        path = path.substring(resourcePathPrefix.length());
        try {
            entry = root.getRoot().getRepository().info(root.getPath() + "/" + path, -1);
        } catch (SVNException e) {
            throw new IOException("cannot probe " + path, e);
        }
        if (entry == null || entry.getKind() == SVNNodeKind.DIR) {
            return null;
        }
        return createResource(entry, path, null);
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
