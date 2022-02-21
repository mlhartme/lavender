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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.util.Strings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Minimal sushi client for https://docs.gitea.io/en-us/api-usage/#api-guide
 * I know there's https://github.com/zeripath/java-gitea-api, but I didn't find a way to stream raw file results.
 */
public class GiteaScmRoot extends ScmRoot {
    public static GiteaScmRoot create(World world, URI uri, String at, String token) throws IOException {
        String uriPath;
        int idx;
        String project;
        String repository;

        uriPath = Strings.removeLeft(uri.getPath(), "/");
        idx = uriPath.indexOf('/');
        if (idx < 0) {
            throw new IOException("invalid url for gitea repository: " + uri);
        }
        project = uriPath.substring(0, idx);
        repository = Strings.removeRight(uriPath.substring(idx + 1), ".git");
        return new GiteaScmRoot((HttpNode) world.validNode("https://" + uri.getHost() + "/api/v1"), project, repository, at, token);
    }

    private final HttpNode root;
    private final String token;
    private final String organization;
    private final String repository;
    private final String ref;

    public GiteaScmRoot(HttpNode root, String organization, String repository, String ref, String token) {
        this.root = root;
        this.token = token;
        this.organization = organization;
        this.repository = repository;
        this.ref = ref;
    }

    public String getOrigin() {
        return root.toString();
    }

    public void writeTo(String path, OutputStream dest) throws IOException {
        HttpNode node;

        node = root.join("repos", organization, repository, "raw", path);
        if (token != null) {
            node = node.withHeaders(HeaderList.of("Authorization", "token " + token));
        }
        node = node.withParameter("ref", ref);
        node.copyFileTo(dest, 0);
    }

    public byte[] read(String path) throws IOException {
        ByteArrayOutputStream dest;

        dest = new ByteArrayOutputStream();
        writeTo(path, dest);
        return dest.toByteArray();
    }
}
