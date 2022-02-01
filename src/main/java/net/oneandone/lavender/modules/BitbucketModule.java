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
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitbucketModule extends Module<BitbucketEntry> {
    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String branchOrTag;

    /** this is the path from the lavender.properties (SvnModule doesn't have this field - it's appended to the svnurl instead */
    private final String accessPathPrefix;

    /** may be null */
    private final PustefixJarConfig config;

    private String loadedRevision;
    private BitbucketContentMap contentMap;

    // CHECKSTYLE:OFF
    public BitbucketModule(Bitbucket bitbucket, String project, String repository, String branchOrTag, String accessPathPrefix,
                           String name, ModuleProperties descriptorOpt, boolean lavendelize,
                           String resourcePathPrefix, String targetPathPrefix, Filter filter, PustefixJarConfig config) {
        super(bitbucket.getOrigin(project, repository), name, descriptorOpt, lavendelize, resourcePathPrefix, targetPathPrefix, filter);

        if (!accessPathPrefix.isEmpty() && !accessPathPrefix.endsWith("/")) {
            throw new IllegalArgumentException(accessPathPrefix);
        }
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.branchOrTag = branchOrTag;
        this.accessPathPrefix = accessPathPrefix;
        this.config = config;

        this.loadedRevision = null;
        this.contentMap = null;
    }
    // CHECKSTYLE:ON

    @Override
    protected Map<String, BitbucketEntry> loadEntries() throws IOException {
        List<String> accessPaths;
        Map<String, BitbucketEntry> result;
        Filter filter;
        String publicPath;
        String relativeAccessPath;

        loadedRevision = bitbucket.latestCommit(project, repository, branchOrTag);
        if (loadedRevision == null) {
            throw new IOException("cannot determine last commit, project="
                    + project + ", repository=" + repository + ", branchOrTag=" + branchOrTag);
        }
        accessPaths = bitbucket.files(project, repository, loadedRevision);
        filter = getFilter();
        result = new HashMap<>();
        if (contentMap == null) {
            contentMap = new BitbucketContentMap(bitbucket, project, repository, loadedRevision);
        }
        for (String accessPath : accessPaths) {
            if (accessPath.startsWith(accessPathPrefix)) {
                relativeAccessPath = accessPath.substring(accessPathPrefix.length());
                if (filter.matches(relativeAccessPath)) {
                    if (config != null) {
                        publicPath = config.getPath(relativeAccessPath);
                    } else {
                        publicPath = relativeAccessPath;
                    }
                    if (publicPath != null) {
                        result.put(publicPath, new BitbucketEntry(publicPath, accessPath, contentMap));
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected Resource createResource(String resourcePath, BitbucketEntry entry) {
        return new BitbucketResource(bitbucket, project, repository, resourcePath, entry, loadedRevision);
    }
}
