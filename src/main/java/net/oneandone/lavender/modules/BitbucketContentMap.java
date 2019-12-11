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

    /** path -> last modified commit hash */
    private final Map<String, String> files;

    /** dirs where last modified did not return files: path -> last modified commit hash of the dir */
    private final Map<String, String> fallbackDirectories;

    public BitbucketContentMap(Bitbucket bitbucket, String project, String repository, String at) {
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.at = at;
        this.files = new HashMap<>();
        this.fallbackDirectories = new HashMap<>();
    }

    public String lookup(String path) throws IOException {
        String contentId;
        int idx;
        String directory;
        String direcotoryId;

        contentId = files.get(path);
        if (contentId == null) {
            idx = path.lastIndexOf('/');
            directory = idx == -1 ? "" : path.substring(0, idx);

            contentId = fallbackDirectories.get(directory);
            if (contentId != null) {
                System.out.println("using fallback id: " + path + " " + contentId);
            } else {
                System.out.println("last-modified " + directory + " ...");
                direcotoryId = bitbucket.lastModified(project, repository, directory, at, files);
                System.out.println("last-modified " + directory + " " + files.size());
                contentId = files.get(path);
                if (contentId == null) {
                    System.out.println("adding fallbackDirectory: " + path + " " + direcotoryId);
                    fallbackDirectories.put(directory, direcotoryId);
                    contentId = direcotoryId;
                }
            }
        }
        return contentId + "@" + path;
    }

    public int size() {
        return files.size();
    }
}
