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
package net.oneandone.lavender.scm;

import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class BitbucketScmRoot extends ScmRoot {
    public static BitbucketScmRoot create(World world, URI uri, UsernamePassword up, String at) throws NodeInstantiationException {
        String uriPath;
        int idx;
        String project;
        String repository;

        uriPath = Strings.removeLeft(uri.getPath(), "/");
        idx = uriPath.indexOf('/');
        project = uriPath.substring(0, idx);
        repository = Strings.removeRight(uriPath.substring(idx + 1), ".git");
        return new BitbucketScmRoot(Bitbucket.create(world, uri.getHost(), up), project, repository, at);
    }

    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String at;

    public BitbucketScmRoot(Bitbucket bitbucket, String project, String repository, String at) {
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.at = at;
    }

    public String getOrigin() {
        return bitbucket.getOrigin(project, repository);
    }

    public void writeTo(String path, OutputStream dest) throws IOException {
        bitbucket.writeTo(project, repository, path, at, dest);
    }
}
