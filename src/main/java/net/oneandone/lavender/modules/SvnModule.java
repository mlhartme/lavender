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
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.svn.SvnFilesystem;
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

    public SvnModule(Filter filter, String type, Index oldIndex, Index index, Node indexFile, SvnNode root,
                     boolean lavendelize, String pathPrefix, String folder) {
        super(filter, type, folder, lavendelize, pathPrefix);
        this.root = root;
        this.oldIndex = oldIndex;
        this.index = index;
        this.indexFile = indexFile;
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
            throw new RuntimeException("todo", e);
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
                Node node;

                entry = base.next();
                path = entry.getRelativePath();
                label = oldIndex.lookup(path);
                if (label != null && label.getLavendelizedPath().equals(Long.toString(entry.getRevision()))) {
                    md5 = label.md5();
                    index.add(label);
                } else {
                    md5 = null;
                }
                node = root.join(path);
                if (entry.getSize() > Integer.MAX_VALUE) {
                    throw new UnsupportedOperationException("file too big: " + entry.getRelativePath());
                }
                return new SvnResource(SvnModule.this, entry.getRevision(),
                        path, (int) entry.getSize(), entry.getDate().getTime(), node, md5);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
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
