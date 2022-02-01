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

import net.oneandone.lavender.config.Secrets;
import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Used for index modules to fetch resources.
 */
public abstract class ScmRoot {
    public static ScmRoot create(World world, String urlstr, String at, Secrets secrets) throws IOException {
        URI uri;
        UsernamePassword up;
        String uriPath;
        String project;
        String repository;
        int idx;
        String host;

        up = secrets.get(urlstr);
        uri = URI.create(urlstr);
        if (!uri.getScheme().equals("git")) {
            throw new IllegalArgumentException("git uri expected, got " + urlstr);
        }
        uri = URI.create(uri.getSchemeSpecificPart());
        host = uri.getHost();
        if (!host.contains("bitbucket")) {
            throw new UnsupportedOperationException("TODO: " + host);
        }
        uriPath = Strings.removeLeft(uri.getPath(), "/");
        idx = uriPath.indexOf('/');
        project = uriPath.substring(0, idx);
        repository = Strings.removeRight(uriPath.substring(idx + 1), ".git");
        return new BitbucketScmRoot(Bitbucket.create(world, host, up), project, repository, at);
    }

    public ScmRoot() {
    }

    public abstract String getOrigin();
    public abstract void writeTo(String path, OutputStream dest) throws IOException;

    //--

    public static class BitbucketScmRoot extends ScmRoot {
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
}
