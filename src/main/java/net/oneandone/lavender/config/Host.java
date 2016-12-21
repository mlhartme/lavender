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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.metadata.annotation.Option;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Type
public class Host {
    public static Host localhost(FileNode basedir) throws UnknownHostException {
        return new Host(InetAddress.getLocalHost().getHostName(), null, basedir.getURI().toString());
    }

    @Value
    private String name;

    /** Specify login XOR uri */
    @Option
    private String login;

    /** Specify login XOR uri */
    @Option
    private String uri;

    public Host() {
        this(null, null, null);
    }

    public Host(String name, String login, String uri) {
        this.name = name;
        this.login = login;
        this.uri = uri;
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /** do not call directly, use pool.connect instead. */
    public Connection connect(World world, String lockContent, int wait) throws IOException {
        Node node;

        if (login == null && uri == null) {
            throw new IllegalStateException("missing login or uri");
        }
        if (login != null && uri != null) {
            throw new IllegalStateException("cannot use both login and uri");
        }

        if (login != null) {
            node = world.validNode("ssh://" + login + "@" + name);
        } else {
            node = world.validNode(uri);
        }
        return lockContent == null ? Connection.openSimple(this, node) : Connection.openLocked(this, node, "tmp/lavender.lock", lockContent, wait);
    }

    public String toString() {
        return "[" + name + "]";
    }
}
