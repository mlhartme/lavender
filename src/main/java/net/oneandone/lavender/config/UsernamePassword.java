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
package net.oneandone.lavender.config;

import java.net.URI;

public class UsernamePassword {
    public static final UsernamePassword ANONYMOUS = new UsernamePassword("", "");

    public final String username;
    public final String password;

    public UsernamePassword(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public URI add(URI fullUri) {
        URI uri;
        String port;

        if (this == ANONYMOUS) {
            return fullUri;
        }

        uri = URI.create(fullUri.getSchemeSpecificPart());
        if (uri.getPort() == -1) {
            port = "";
        } else {
            port = ":" + Integer.toString(uri.getPort());
        }
        return URI.create(fullUri.getScheme() + ":" + uri.getScheme() + "://" + username + ":" + password + "@" + uri.getHost() + port + uri.getPath());
    }

    public int hashCode() {
        return password.hashCode();
    }

    public boolean equals(Object obj) {
        UsernamePassword up;

        if (obj instanceof UsernamePassword) {
            up = (UsernamePassword) obj;
            return username.equals(up.username) && password.equals(up.password);
        }
        return false;
    }
}
