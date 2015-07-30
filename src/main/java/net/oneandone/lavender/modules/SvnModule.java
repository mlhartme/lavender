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

import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.svn.SvnNode;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Extracts resources from svn */
public class SvnModule extends Module<SvnEntry> {
    public static SvnModule create(String type, String name, Node indexFile, SvnNode root, boolean lavendelize, String resourcePathPrefix,
                     String targetPathPrefix, Filter filter, JarConfig jarConfig) throws IOException {
        Index index;
        long lastModifiedModule;

        if (indexFile.exists()) {
            index = Index.load(indexFile);
            lastModifiedModule = 0; // TODO
        } else {
            index = new Index();
            lastModifiedModule = 0;
        }
        return new SvnModule(type, name, indexFile, index, lastModifiedModule, root, lavendelize, resourcePathPrefix, targetPathPrefix, filter, jarConfig);
    }

    private static final Logger LOG = LoggerFactory.getLogger(SvnModule.class);

    private final SvnNode root;

    /**
     * Maps svn paths (relative to root (i.e. without modulePrefix), with jarConfig applied) to revision numbers and md5 hashes.
     * Contains only entries where the md5 sum is known.
     */
    private Index index;
    private final Node indexFile;
    private Map<String, SvnEntry> lastScan;
    private long lastModifiedRepository;
    private long lastModifiedModule;

    /** may be null */
    private final JarConfig jarConfig;

    public SvnModule(String type, String name, Node indexFile, Index index, long lastModifiedModule, SvnNode root, boolean lavendelize, String resourcePathPrefix,
                     String targetPathPrefix, Filter filter, JarConfig jarConfig) throws IOException {
        super(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
        this.root = root;
        this.indexFile = indexFile;
        this.index = index;
        this.lastModifiedModule = lastModifiedModule;
        this.lastModifiedRepository = 0;
        this.jarConfig = jarConfig;
    }

    protected Map<String, SvnEntry> scan(final Filter filter) throws SVNException {
        SVNRepository repository;
        long modifiedRepository;
        long modifiedModule;

        repository = root.getRoot().getRepository();
        modifiedRepository = repository.getLatestRevision();
        if (modifiedRepository == lastModifiedRepository) {
            LOG.info("no changes in repository: " + modifiedRepository);
            if (lastScan == null) {
                throw new IllegalStateException();
            }
            return lastScan;
        }
        lastModifiedRepository = modifiedRepository;
        modifiedModule = getLastModified();
        LOG.info("latest " + root.getURI() + ": " + modifiedModule + " " + modifiedRepository);
        if (modifiedModule == lastModifiedModule) {
            LOG.info("re-using last scan for revision " + modifiedModule);
            if (lastScan == null) {
                throw new IllegalStateException();
            }
            return lastScan;
        }
        LOG.info("new scan: " + modifiedModule + " vs " + lastModifiedModule);
        lastModifiedModule = modifiedModule;
        lastScan = doScan(filter);
        return lastScan;
    }

    private long getLastModified() throws SVNException {
        final List<SVNDirEntry> result;

        result = new ArrayList<>();
        root.getRoot().getClientMananger().getLogClient().doList(root.getSvnurl(), null, SVNRevision.create(lastModifiedRepository),
                false, SVNDepth.EMPTY, SVNDirEntry.DIRENT_ALL, new ISVNDirEntryHandler() {
            @Override
            public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                result.add(dirEntry);
            }
        });
        if (result.size() != 1) {
            throw new IllegalStateException("" + result.size());
        }
        return result.get(0).getRevision();
    }

    protected Map<String, SvnEntry> doScan(final Filter filter) throws SVNException {
        final Map<String, SvnEntry> files;
        final Index oldIndex;

        oldIndex = index;
        index = new Index();
        files = new HashMap<>();
        root.getRoot().getClientMananger().getLogClient().doList(
                root.getSvnurl(), null, SVNRevision.create(lastModifiedRepository), true, SVNDepth.INFINITY,
                SVNDirEntry.DIRENT_KIND + SVNDirEntry.DIRENT_SIZE + SVNDirEntry.DIRENT_TIME + SVNDirEntry.DIRENT_CREATED_REVISION,
                new ISVNDirEntryHandler() {
            @Override
            public void handleDirEntry(SVNDirEntry entry) throws SVNException {
                String path;
                Label label;

                if (entry.getKind() == SVNNodeKind.FILE) {
                    path = entry.getRelativePath();
                    if (filter.matches(path)) {
                        if (jarConfig != null) {
                            path = jarConfig.getPath(path);
                        }
                        if (path != null) {
                            if (entry.getSize() > Integer.MAX_VALUE) {
                                throw new UnsupportedOperationException("file too big: " + path);
                            }
                            files.put(path, SvnEntry.create(entry));
                            label = oldIndex.lookup(path);
                            if (label != null && entry.getRevision() == Long.parseLong(label.getLavendelizedPath())) {
                                index.add(label);
                            }
                        }
                    }
                }
            }
        });
        return files;
    }

    @Override
    protected SvnResource createResource(String resourcePath, SvnEntry entry) {
        String svnPath;

        svnPath = entry.relativePath;
        return new SvnResource(this, entry.revision, lastModifiedRepository /* not module, because paths might already be out-dated */,
                resourcePath, (int) entry.size, entry.time, root.join(svnPath), md5(entry));
    }

    protected byte[] md5(SvnEntry entry) {
        String path;
        Label label;

        path = entry.relativePath;
        if (jarConfig != null) {
            path = jarConfig.getPath(path);
        }
        label = index.lookup(path);
        if (label != null && label.getLavendelizedPath().equals(Long.toString(entry.revision))) {
            return label.md5();
        } else {
            return null;
        }
    }

    public void addIndex(Label label) {
        index.add(label);
    }

    public String uri() {
        return root.getURI().toString();
    }

    public void saveCaches() throws IOException {
        index.save(indexFile);
    }
}
