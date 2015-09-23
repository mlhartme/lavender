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
        return new Host(InetAddress.getLocalHost().getHostName(), System.getProperty("user.name"), basedir.getPath());
    }

    @Value
    private String name;

    @Option
    private Integer port;

    /** or path for if name is localhost */
    @Value
    private String login;

    @Option
    private String path;

    public Host() {
        this(null, null, null);
    }

    public Host(String name, String login, String path) {
        this(name, null, login, path);
    }

    public Host(String name, Integer port, String login, String path) {
        this.name = name;
        this.port = port;
        this.login = login;
        this.path = path;
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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /** do not call directly, use pool.connect instead. */
    public Connection connect(World world, String lockContent, int wait) throws IOException {
        Node node;

        node = world.validNode(getUri());
        return lockContent == null ? Connection.openSimple(this, node) : Connection.openLocked(this, node, "tmp/lavender.lock", lockContent, wait);
    }

    /**
     * @return Return URI of host. Use ssh schema if none is given
     */
    private String getUri() {
        int idx;
        String uri = name;

        idx = uri.indexOf("@");
        if (idx == -1 && login != null && login.length() > 0) {
            uri = login + "@" + uri;
        }
        idx = uri.indexOf("://");
        if (idx == -1) {
            uri = "ssh://" + uri;
        }
        if (port != null) {
            uri += port;
        }
        if (path != null) {
            uri += "/" + path;
        }
        return uri;
    }

    public String toString() {
        return "[" + name + "]";
    }
}
