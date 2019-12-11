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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BitbucketContentMap {
    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String at;


    private final Map<String, String> map;

    public BitbucketContentMap(Bitbucket bitbucket, String project, String repository, String at) {
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.at = at;
        this.map = new HashMap<>();
    }

    public String lookup(String path) throws IOException {
        String contentId;
        int idx;
        String directory;

        contentId = map.get(path);
        if (contentId == null) {
            idx = path.lastIndexOf('/');
            directory = idx == -1 ? "" : path.substring(0, idx);
            System.out.println("last-modified " + directory + " ...");
            bitbucket.lastModified(project, repository, directory, at, map);
            System.out.println("last-modified " + directory + " " + map.size());
            contentId = map.get(path);
            if (contentId == null) {
                throw new IllegalStateException(path);
            }
        }
        return contentId + "@" + path;
    }

    public int size() {
        return map.size();
    }
}
