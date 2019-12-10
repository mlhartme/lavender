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
    private final String accessPathPrefix;

    /** may be null */
    private final PustefixJarConfig config;

    private String loadedRevision;

    // CHECKSTYLE:OFF
    public BitbucketModule(Bitbucket bitbucket, String project, String repository, String branchOrTag, String accessPathPrefix,
                           String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter, PustefixJarConfig config) {
        super(bitbucket.getOrigin(project, repository), Module.TYPE, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);

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
    }
    // CHECKSTYLE:ON

    @Override
    protected Map<String, BitbucketEntry> loadEntries() throws IOException {
        List<String> accessPaths;
        Map<String, BitbucketEntry> result;
        Filter filter;
        String publicPath;
        String relativeAccessPath;
        Map<String, String> contentMap; // accessPath -> contentId

        loadedRevision = bitbucket.latestCommit(project, repository, branchOrTag);
        if (loadedRevision == null) {
            throw new IOException("cannot determine last commit, project="
                    + project + ", repository=" + repository + ", branchOrTag=" + branchOrTag);
        }
        accessPaths = bitbucket.files(project, repository, loadedRevision);
        filter = getFilter();
        result = new HashMap<>();
        contentMap = new HashMap<>();
        for (String accessPath : accessPaths) {
            if (accessPath.startsWith(accessPathPrefix)) {
                relativeAccessPath = accessPath.substring(accessPathPrefix.length());
                if (filter.matches(relativeAccessPath)) {
                    if (config != null) {
                        publicPath = config.getPath(relativeAccessPath);
                    } else {
                        publicPath = accessPath;
                    }
                    if (publicPath != null) {
                        result.put(publicPath, new BitbucketEntry(publicPath, accessPath,
                                contentId(bitbucket, project, repository, loadedRevision, accessPath, contentMap)));
                    }
                }
            }
        }
        return result;
    }

    private static String contentId(Bitbucket bitbucket, String project, String repository, String at, String path,
                                    Map<String, String> contentMap) throws IOException {
        String contentId;
        int idx;
        String directory;

        contentId = contentMap.get(path);
        if (contentId == null) {
            idx = path.lastIndexOf('/');
            directory = idx == -1 ? "" : path.substring(0, idx);
            System.out.println("> lastModified " + directory + " " + contentMap.size());
            bitbucket.lastModified(project, repository, directory, at, contentMap);
            System.out.println("< lastModified " + directory + " " + contentMap.size());
            contentId = contentMap.get(path);
            if (contentId == null) {
                throw new IllegalStateException(path);
            }
        }
        return contentId + "@" + path;
    }

    @Override
    protected Resource createResource(String resourcePath, BitbucketEntry entry) {
        return new BitbucketResource(bitbucket, project, repository, resourcePath, entry, loadedRevision);
    }
}
