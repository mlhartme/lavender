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

import net.oneandone.lavender.index.Util;
import net.oneandone.sushi.fs.MkfileException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.io.LineFormat;
import net.oneandone.sushi.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Extracts resources from svn */
public class SvnModule extends Module<SvnEntry> {
    public static SvnModule create(String type, String name, Node cacheFile, SvnNode root, long pinnedRevision, boolean lavendelize, String resourcePathPrefix,
                     String targetPathPrefix, Filter filter, JarConfig jarConfig) throws IOException {
        Map<String, SvnEntry> entries;
        long lastModifiedModule;
        String line;
        SvnEntry entry;

        entries = new HashMap<>();
        lastModifiedModule = 0;
        if (cacheFile.exists()) {
            try (Reader reader = cacheFile.newReader();
                 LineReader lines = new LineReader(reader, new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.ALL))) {
                line = lines.next();
                if (line != null) {
                    lastModifiedModule = Long.parseLong(line);
                    while (true) {
                        line = lines.next();
                        if (line == null) {
                            break;
                        }
                        try {
                            entry = SvnEntry.parse(line);
                        } catch (RuntimeException e) {
                            throw new RuntimeException(cacheFile + ": failed to parse cache line '" + line + "': " + e.getMessage(), e);
                        }
                        entries.put(entry.publicPath, entry);
                    }
                }
            }
        }
        return new SvnModule(type, name, cacheFile, entries, lastModifiedModule, root, pinnedRevision, lavendelize, resourcePathPrefix, targetPathPrefix, filter, jarConfig);
    }

    private static final Logger LOG = LoggerFactory.getLogger(SvnModule.class);

    private final SvnNode root;
    /** Revision you want to pin this module to, -1 for no pinning */
    private final long pinnedRevision;

    /**
     * Maps svn paths (relative to root (i.e. without modulePrefix), with jarConfig applied) to revision numbers and md5 hashes.
     * Contains only entries where the md5 sum is known.
     */
    private Map<String, SvnEntry> entries;
    private final Node indexFile;
    /** if pinnedRevision == 1: lastModified reported by repository, otherwise pinnedRevision */
    private long lastModifiedRepository;
    private long lastModifiedModule;

    /** may be null */
    private final JarConfig jarConfig;

    public SvnModule(String type, String name, Node indexFile, Map<String, SvnEntry> entries, long lastModifiedModule, SvnNode root,
                     long pinnedRevision, boolean lavendelize, String resourcePathPrefix,
                     String targetPathPrefix, Filter filter, JarConfig jarConfig) {
        super(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
        this.root = root;
        this.pinnedRevision = pinnedRevision;
        this.indexFile = indexFile;
        this.entries = entries;
        this.lastModifiedModule = lastModifiedModule;
        this.lastModifiedRepository = 0;
        this.jarConfig = jarConfig;
    }

    public SvnNode getRoot() {
        return root;
    }

    protected Map<String, SvnEntry> doScan(final Filter filter) throws SVNException {
        SVNRepository repository;
        long modifiedRepository;
        long modifiedModule;

        repository = root.getRoot().getRepository();
        modifiedRepository = pinnedRevision == -1 ? repository.getLatestRevision() : pinnedRevision;
        if (modifiedRepository == lastModifiedRepository) {
            LOG.info("no changes in repository: " + modifiedRepository);
            return entries;
        }
        lastModifiedRepository = modifiedRepository;
        modifiedModule = getLastModified();
        if (modifiedModule == lastModifiedModule) {
            LOG.info(root.getUri() + ": re-using doScan for revision " + modifiedModule);
            return entries;
        }
        LOG.info(root.getUri() + ": doScan " + lastModifiedModule + " is out-dated, rescanning revision " + modifiedModule);
        entries = doSvnScan(filter);
        lastModifiedModule = modifiedModule;
        return entries;
    }

    private long getLastModified() throws SVNException {
        final List<SVNDirEntry> result;

        if (pinnedRevision != -1) {
            return pinnedRevision;
        }
        result = new ArrayList<>();
        root.getRoot().getClientMananger().getLogClient().doList(root.getSvnurl(), null, SVNRevision.create(lastModifiedRepository),
                false, SVNDepth.EMPTY, SVNDirEntry.DIRENT_ALL, new ISVNDirEntryHandler() {
                    @Override
                    public void handleDirEntry(SVNDirEntry dirEntry) {
                        result.add(dirEntry);
                    }
                });
        if (result.size() != 1) {
            throw new IllegalStateException("" + result.size());
        }
        return result.get(0).getRevision();
    }

    protected Map<String, SvnEntry> doSvnScan(final Filter filter) throws SVNException {
        final Map<String, SvnEntry> newEntries;

        newEntries = new HashMap<>();
        root.getRoot().getClientMananger().getLogClient().doList(
                root.getSvnurl(), null, SVNRevision.create(lastModifiedRepository), false, SVNDepth.INFINITY,
                SVNDirEntry.DIRENT_KIND + SVNDirEntry.DIRENT_SIZE + SVNDirEntry.DIRENT_TIME + SVNDirEntry.DIRENT_CREATED_REVISION,
                new ISVNDirEntryHandler() {
            @Override
            public void handleDirEntry(SVNDirEntry entry) {
                String accessPath;
                String publicPath;
                SvnEntry old;

                if (entry.getKind() == SVNNodeKind.FILE) {
                    accessPath = entry.getRelativePath();
                    if (filter.matches(accessPath)) {
                        if (jarConfig != null) {
                            publicPath = jarConfig.getPath(accessPath);
                        } else {
                            publicPath = accessPath;
                        }
                        if (publicPath != null) {
                            if (entry.getSize() > Integer.MAX_VALUE) {
                                throw new UnsupportedOperationException("file too big: " + publicPath + " " + entry.getSize());
                            }
                            old = entries.get(publicPath);
                            if (old != null && entry.getRevision() == old.revision) {
                                newEntries.put(publicPath, old);
                            } else {
                                newEntries.put(publicPath, new SvnEntry(publicPath, accessPath, entry.getRevision(),
                                        (int) entry.getSize()));
                            }
                        }
                    }
                }
            }
        });
        return newEntries;
    }

    @Override
    protected SvnResource createResource(String resourcePath, SvnEntry entry) {
        return new SvnResource(this, resourcePath, entry, lastModifiedRepository /* not module, because paths might already be out-dated */);
    }

    public String uri() {
        return root.getUri().toString();
    }

    public void saveCaches() throws IOException {
        FileNode parent;
        FileNode tmp;

        // first write to a temp file, then move it (which is atomic) because
        // * no corruption by crashed/killed processes
        // * works for multiple users as long as the cache directory has the proper permissions
        parent = (FileNode) indexFile.getParent();
        tmp = Util.newTmpFile(parent);
        try (Writer dest = tmp.newWriter()) {
            dest.write(Long.toString(lastModifiedModule));
            dest.write('\n');
            for (SvnEntry entry : entries.values()) {
                dest.write(entry.toString());
                dest.write('\n');
            }
        }
        tmp.move(indexFile, true);
    }
}
