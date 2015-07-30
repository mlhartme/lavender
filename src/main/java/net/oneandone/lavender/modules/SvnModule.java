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
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Extracts resources from svn */
public class SvnModule extends Module<SVNInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(SvnModule.class);

    private final SvnNode root;

    /**
     * Maps svn paths (relative to root (i.e. without modulePrefix), with jarConfig applied) to revision numbers and md5 hashes.
     * Contains only entries where the md5 sum is known.
     */
    private Index index;
    private final Node indexFile;
    private Map<String, SVNInfo> lastScan;
    private long lastScanRevision;

    /** may be null */
    private JarConfig jarConfig;

    public SvnModule(String type, String name, Index index, Node indexFile, SvnNode root, boolean lavendelize, String resourcePathPrefix,
                     String targetPathPrefix, Filter filter, JarConfig jarConfig) {
        super(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
        this.root = root;
        this.index = index;
        this.indexFile = indexFile;
        this.jarConfig = jarConfig;
    }

    protected Map<String, SVNInfo> scan(final Filter filter) throws SVNException {
        SVNRepository repository;
        long latest;

        repository = root.getRoot().getRepository();
        latest = repository.getLatestRevision();
        LOG.info("latest " + root.getURI() + ": " + latest);
        if (lastScan != null) {
            if (latest == lastScanRevision) {
                LOG.info("re-using last scan for revision " + latest);
                return lastScan;
            } else if (repository.log(new String[] { root.getPath() } , lastScanRevision , latest, true , true, 1, null ) == 0) {
                LOG.info("no changes in " + root.getPath() + "between " + lastScanRevision + " and " + latest);
                lastScanRevision = latest;
                return lastScan;
            }
            LOG.info("new scan: " + latest + " vs " + lastScanRevision);
        }
        lastScanRevision = latest;
        lastScan = doScan(filter);
        return lastScan;
    }

    protected Map<String, SVNInfo> doScan(final Filter filter) throws SVNException {
        final Map<String, SVNInfo> files;
        final Index oldIndex;

        oldIndex = index;
        index = new Index();
        files = new HashMap<>();
        // looks silly to use the WCClient even though I don't have a checkout. But get LogClient doesn't return enough information
        // to read files for the given revision (because the returned paths don't point to the origin of a copy
        root.getRoot().getClientMananger().getWCClient().doInfo(root.getSvnurl(), null, null, SVNDepth.INFINITY, new ISVNInfoHandler() {
            @Override
            public void handleInfo(SVNInfo info) throws SVNException {
                String path;
                Label label;

                if (info.getKind() == SVNNodeKind.FILE) {
                    path = relativePath(info);
                    if (filter.matches(path)) {
                        if (jarConfig != null) {
                            path = jarConfig.getPath(path);
                        }
                        if (path != null) {
                            if (info.getRepositorySize() > Integer.MAX_VALUE) {
                                throw new UnsupportedOperationException("file too big: " + path);
                            }
                            files.put(path, info);
                            label = oldIndex.lookup(path);
                            if (label != null && info.getCommittedRevision().getNumber() == Long.parseLong(label.getLavendelizedPath())) {
                                index.add(label);
                            }
                        }
                    }
                }
            }
        });
        return files;
    }

    private String relativePath(SVNInfo info) {
        return Strings.removeLeft(info.getURL().toDecodedString(), root.getSvnurl().toDecodedString() + "/");
    }

    @Override
    protected SvnResource createResource(String resourcePath, SVNInfo info) {
        String svnPath;

        svnPath = relativePath(info);
        return new SvnResource(this, info.getCommittedRevision().getNumber(), resourcePath,
                (int) info.getRepositorySize(), info.getCommittedDate().getTime(), root.join(svnPath), md5(info));
    }

    protected byte[] md5(SVNInfo info) {
        String path;
        Label label;

        path = relativePath(info);
        if (jarConfig != null) {
            path = jarConfig.getPath(path);
        }
        label = index.lookup(path);
        if (label != null && label.getLavendelizedPath().equals(Long.toString(info.getCommittedRevision().getNumber()))) {
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
