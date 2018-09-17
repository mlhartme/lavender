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
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

/** Extracts resources from svn. TODO: module cache is not invalidated if the filter configuration is changed. */
public class SvnModule extends Module<SvnEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(SvnModule.class);

    private final FileNode cacheFile;

    private final SvnNode root;
    /** Revision you want to pin this module to, -1 for no pinning */
    private final long pinnedRevision;

    /** -1 for unknown; otherwise: if pinnedRevision == -1: lastModified reported by repository, otherwise pinnedRevision */
    private long lastModifiedRepository;
    private long lastModifiedModule;

    /** may be null */
    private final JarConfig jarConfig;

    public SvnModule(String type, String name, FileNode cacheFile, SvnNode root,
                     long pinnedRevision, boolean lavendelize, String resourcePathPrefix,
                     String targetPathPrefix, Filter filter, JarConfig jarConfig) {
        super(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
        this.cacheFile = cacheFile;
        this.root = root;
        this.pinnedRevision = pinnedRevision;
        this.lastModifiedRepository = -1;
        this.lastModifiedModule = -1;
        this.jarConfig = jarConfig;
    }

    public SvnNode getRoot() {
        return root;
    }

    protected Map<String, SvnEntry> loadEntries() throws IOException {
        Map<String, SvnEntry> cachedEntries;
        long nextModifiedRepository;
        long nextModifiedModule;
        Map<String, SvnEntry> entries;

        try {
            cachedEntries = loadedEntries(); // != null implies that there is a cache file
            if (cachedEntries == null) {
                cachedEntries = loadCachedEntriesOpt();
            }
            nextModifiedRepository = getRepositoryLastModified();
            if (nextModifiedRepository != lastModifiedRepository) {
                nextModifiedModule = getModuleLastModified();
            } else {
                nextModifiedModule = lastModifiedModule;
            }

            if (lastModifiedModule == nextModifiedModule) {
                if (cachedEntries == null) {
                    throw new IllegalStateException("" + lastModifiedModule);
                }
                LOG.info("no changes in repository: " + nextModifiedRepository + " " + nextModifiedModule + " -> using cached entries");
                return cachedEntries;
            }

            LOG.info(root.getUri() + ": entries " + lastModifiedModule + " is out-dated, reloading entries for revision " + nextModifiedModule);
                entries = loadServerEntries(nextModifiedRepository);
            saveCache(entries);
            lastModifiedRepository = nextModifiedRepository;
            lastModifiedModule = nextModifiedModule;
            return entries;
        } catch (SVNException|IOException e) {
            throw new IOException("failed to load entries for module '" + getName() + "': " + e.getMessage(), e);
        }
    }

    private long getRepositoryLastModified() throws SVNException {
        return pinnedRevision == -1 ? root.getRoot().getRepository().getLatestRevision() : pinnedRevision;
    }

    /** last modified revision of the modules directory */
    private long getModuleLastModified() throws SVNException {
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

    private void saveCache(Map<String, SvnEntry> entries) throws IOException {
        try (Writer dest = cacheFile.newWriter()) {
            dest.write(Long.toString(lastModifiedModule));
            dest.write('\n');
            for (SvnEntry entry : entries.values()) {
                dest.write(entry.toString());
                dest.write('\n');
            }
        }
    }

    /** null if no cache exists */
    private Map<String, SvnEntry> loadCachedEntriesOpt() throws IOException {
        Map<String, SvnEntry> entries;
        String line;
        SvnEntry entry;

        if (!cacheFile.exists()) {
            return null;
        }
        entries = new TreeMap<>();
        try (Reader reader = cacheFile.newReader();
             LineReader lines = new LineReader(reader, new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.ALL))) {
            line = lines.next();
            if (line != null) {
                lastModifiedModule = Long.parseLong(line);
                while ((line = lines.next()) != null) {
                    try {
                        entry = SvnEntry.parse(line);
                    } catch (RuntimeException e) {
                        throw new RuntimeException(cacheFile + ": failed to parse cache line '" + line + "': " + e.getMessage(), e);
                    }
                    entries.put(entry.publicPath, entry);
                }
            }
        }
        return entries;
    }

    private Map<String, SvnEntry> loadServerEntries(long revision) throws SVNException {
        final Filter filter;
        final Map<String, SvnEntry> entries;

        filter = getFilter();
        entries = new TreeMap<>();
        root.getRoot().getClientMananger().getLogClient().doList(
                root.getSvnurl(), null, SVNRevision.create(revision), false, SVNDepth.INFINITY,
                SVNDirEntry.DIRENT_KIND + SVNDirEntry.DIRENT_SIZE + SVNDirEntry.DIRENT_TIME + SVNDirEntry.DIRENT_CREATED_REVISION,
                new ISVNDirEntryHandler() {
            @Override
            public void handleDirEntry(SVNDirEntry entry) {
                String accessPath;
                String publicPath;

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
                            entries.put(publicPath, new SvnEntry(publicPath, accessPath, entry.getRevision()));
                        }
                    }
                }
            }
        });
        return entries;
    }

    @Override
    protected SvnResource createResource(String resourcePath, SvnEntry entry) {
        return new SvnResource(this, resourcePath, entry, lastModifiedRepository /* not module, because paths might already be out-dated */);
    }

    public String uri() {
        return root.getUri().toString();
    }
}
