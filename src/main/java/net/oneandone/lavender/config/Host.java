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
package net.oneandone.lavender.config;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.net.URI;
import java.net.URISyntaxException;

@Type
public class Host {
    private static final String LOCALHOST = "localhost";

    public static Host remote(String name, String login) {
        return new Host(name, login);
    }

    public static Host local(FileNode basedir) {
        return new Host(LOCALHOST, basedir.getAbsolute());
    }

    @Value
    private String name;

    /** or path for if name is localhost */
    @Value
    private String login;

    public Host() {
        this(null, null);
    }

    public Host(String name, String login) {
        this.name = name;
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    /** @return root node of this host */
    public Node open(World world) throws NodeInstantiationException {
        if (name.equals(LOCALHOST)) {
            return world.file(login);
        } else {
            return world.validNode("ssh://" + login + "@" + name);
        }
    }

    public String toString() {
        return "[" + name + "]";
    }
}
