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
import net.oneandone.sushi.fs.file.FileNode;

import java.net.URI;
import java.net.URISyntaxException;

public class Host {
    public static Host remote(String name, String login) {
        try {
            return new Host(name, new URI("ssh://" + login + "@" + name));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Host local(FileNode basedir) {
        return new Host("localhost", basedir.getURI());
    }

    public final String name;

    public final URI uri;

    public Host(String name, URI uri) {
        this.name = name;
        this.uri = uri;
   }

    /** @return root node of this host */
    public Node open(World world) throws NodeInstantiationException {
        return world.node(uri);
    }

    public String toString() {
        return "[" + name + "]";
    }
}
