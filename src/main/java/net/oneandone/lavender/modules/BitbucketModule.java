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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class BitbucketModule extends Module<String> {
    public static BitbucketModule create(World world, String project, String repository, String branch,
                                         String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) throws IOException {
        return new BitbucketModule((HttpNode) world.validNode("http://bitbucket.1and1.org:7990/rest/api/1.0"), project, repository, branch,
                name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
    }

    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String branch;

    private String revision;

    public BitbucketModule(HttpNode root, String project, String repository, String branch,
                           String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        super(Module.TYPE, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter);

        this.bitbucket = new Bitbucket(root);
        this.project = project;
        this.repository = repository;
        this.branch = branch;

        this.revision = null;
    }

    @Override
    protected Map<String, String> loadEntries() throws IOException {
        Map<String, String> result;
        Iterator<Map.Entry<String, String>> iter;
        Map.Entry<String, String> entry;
        Filter filter;

        revision = bitbucket.latestCommit(project, repository, branch);
        result = bitbucket.changes(project, repository, revision);
        iter = result.entrySet().iterator();
        filter = getFilter();
        while (iter.hasNext()) {
            entry = iter.next();
            if (!filter.matches(entry.getKey())) {
                iter.remove();
            }
        }
        return result;
    }

    @Override
    protected Resource createResource(String path, String contentId) {
        return new BitbucketResource(bitbucket, project, repository, path, revision, contentId);
    }
}
