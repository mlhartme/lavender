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

import net.oneandone.lavender.config.Secrets;
import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpFilesystem;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

/**
 * Used for IndexedModules to fetch resources.
 */
public abstract class ScmRoot {
    public static ScmRoot create(World world, String urlstr, String at, Secrets secrets, List<String> bitbucketHosts) throws IOException {
        UsernamePassword up;
        URI uri;
        String token;
        String wirelog;

        up = secrets.get(urlstr);
        uri = URI.create(urlstr);
        if (!uri.getScheme().equals("git")) {
            throw new IllegalArgumentException("git uri expected, got " + urlstr);
        }
        uri = URI.create(uri.getSchemeSpecificPart());
        wirelog = System.getProperty("lavender.wirelog");
        if (wirelog != null) {
            HttpFilesystem.wireLog(wirelog);
        }

        if (bitbucketHosts.contains(uri.getHost())) {
            return BitbucketScmRoot.create(world, uri, up, at);
        } else {
            if (up != null) {
                if (!"token".equals(up.username)) {
                    throw new IOException("username has to be token: " + up.username);
                }
                token = up.password;
            } else {
                token = null;
            }
            return GiteaScmRoot.create(world, uri, at, token);
        }
    }

    public ScmRoot() {
    }

    public abstract String getOrigin();
    public abstract void writeTo(String path, OutputStream dest) throws IOException;

    //--

}
