/**
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
package net.oneandone.lavender.publisher.config;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

public class Host {
    /** null for localhost (login is ignored in this case, and open returns FileNodes) */
    public final String name;
    public final String login;

    /** where docroots of the various domains reside */
    private final String docrootbase;
    private final String indexbase;

    public Host(String name, String login, String docrootbase, String indexbase) {
        if (docrootbase.startsWith("/") || docrootbase.endsWith("/")) {
            throw new IllegalArgumentException(docrootbase);
        }
        if (indexbase.startsWith("/") || indexbase.endsWith("/")) {
            throw new IllegalArgumentException(indexbase);
        }
        this.name = name;
        this.login = login;
        this.docrootbase = docrootbase;
        this.indexbase = indexbase;
    }

    /** @return root node of this host */
    public Node open(World world) throws NodeInstantiationException {
        if (name != null) {
            return world.validNode("ssh://" + login + "@" + name);
        } else {
            return world.file("/");
        }
    }

    public Node docroot(Node root, String suffix) throws NodeInstantiationException {
        return root.join(docrootbase + suffix);
    }

    public Node index(Node root, String indexName) throws NodeInstantiationException {
        return root.join(indexbase, indexName);
    }

    public String toString() {
        if (name == null) {
            return "[localhost]";
        } else {
            return "[" + login + "@" + name + "]";
        }
    }
}
