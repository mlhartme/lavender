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

import net.oneandone.lavender.modules.PropertiesBase;
import net.oneandone.lavender.modules.Secrets;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshFilesystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class SystemProperties extends PropertiesBase {
    public static SystemProperties load(World world) throws IOException, URISyntaxException {
        return load(file(world), true);
    }

    public static SystemProperties load(Node file, boolean withSsh) throws IOException, URISyntaxException {
        SystemProperties properties;

        properties = properties(file);
        properties.initWorld(withSsh);
        return properties;
    }

    public static Node file(World world) throws IOException {
        String path;
        Node file;
        FileNode parent;

        path = System.getProperty("lavender.properties");
        if (path != null) {
            return world.file(path);
        }
        path = System.getenv("LAVENDER_PROPERTIES");
        if (path != null) {
            return world.file(path);
        }
        parent = world.locateClasspathItem(SystemProperties.class).getParent();
        file = parent.join("lavender.properties");
        if (file.exists()) {
            return file;
        }
        file = world.getHome().join(".lavender.properties");
        if (file.exists()) {
            return file;
        }
        file = world.file("/etc/lavender.properties");
        if (file.exists()) {
            return file;
        }
        throw new IOException("cannot locate lavender properties");
    }

    private static SystemProperties properties(Node file) throws IOException, URISyntaxException {
        java.util.Properties properties;
        List<Node> sshKeys;
        String str;
        FileNode cache;
        Secrets secrets;
        World world;
        Node source;
        Node network;

        world = file.getWorld();
        properties = file.readProperties();
        sshKeys = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("ssh.")) {
                sshKeys.add(file.getWorld().file(eat(properties, key)));
            }
        }
        str = eatOpt(properties,"cache", null);
        if (str == null) {
            cache = world.getHome().join(".cache/lavender");
        } else {
            cache = world.file(str);
        }
        str = eatOpt(properties, "secrets", null);
        if (str == null) {
            source = file.getParent().join("lavender.secrets");
        } else {
            source = world.file(str);
        }
        secrets = Secrets.load(source);
        str = eatOpt(properties, "network", null);
        if (str == null) {
            network = file.getParent().join("network.xml");
        } else {
            network = world.node(str);
        }
        try {
            return new SystemProperties(world, cache, new URI(eat(properties, "svn")), network, secrets, sshKeys);
        } catch (URISyntaxException e) {
            throw new IOException("invalid properties file " + file + ": " + e.getMessage(), e);
        }
    }

    //--

    public final World world;
    private final FileNode cache;
    /** don't store the node, so I can create properties without accessing svn (and thus without svn credentials) */
    public final URI svn;
    public final Node network;
    public final Secrets secrets;
    private final List<Node> sshKeys;

    public SystemProperties(World world, FileNode cache, URI svn, Node network, Secrets secrets, List<Node> sshKeys) {
        this.world = world;
        this.cache = cache;
        this.svn = svn;
        this.network = network;
        this.secrets = secrets;
        this.sshKeys = sshKeys;
    }

    public void initTemp(FileNode temp) throws IOException {
        FileNode parent;

        parent = temp.getParent();
        if (!parent.exists()) {
            parent.mkdir();
            parent.setPermissions("rwxrwxrwx");
        }
        world.setTemp(temp.mkdirOpt());
    }

    private FileNode temp() {
        String str;

        str = System.getenv("LAVENDER_TEMP");
        if (str == null) {
            // make sure that users have individual sub directories - for shared machines
            return world.getTemp().join("lavender", System.getProperty("user.name"));
        } else {
            // TODO: dump this case when we can dump pumama64
            return world.file(str);
        }
    }

    private void initWorld(boolean withSsh) throws IOException {
        SshFilesystem ssh;

        initTemp(temp());
        world.getMemoryFilesystem().setMaxInMemorySize(Integer.MAX_VALUE);
        if (withSsh) {
            ssh = world.getFilesystem("ssh", SshFilesystem.class);
            for (Node node : sshKeys) {
                try {
                    ssh.addIdentity(node, null);
                } catch (Exception e) {
                    throw new IllegalStateException("cannot add identity: " + e.getMessage(), e);
                }
            }
        }
        // disable them for integration tests, because I don't have .ssh on pearl/gems
    }

    public FileNode cacheroot() throws IOException {
        cache.mkdirsOpt();
        return cache;
    }
}
