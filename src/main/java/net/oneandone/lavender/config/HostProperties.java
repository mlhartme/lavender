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
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** configuration comming from the host that runs lavender */
public class HostProperties extends PropertiesBase {
    public static HostProperties load(World world) throws IOException, URISyntaxException {
        return load(file(world), true);
    }

    public static HostProperties load(Node file, boolean withSsh) throws IOException, URISyntaxException {
        HostProperties properties;

        properties = properties(file);
        properties.initWorld(withSsh);
        return properties;
    }

    public static Node file(World world) throws IOException {
        String path;
        Node file;
        FileNode parent;

        path = System.getProperty("lavender.hostproperties");
        if (path != null) {
            return world.file(path);
        }
        path = System.getenv("LAVENDER_HOSTPROPERTIES");
        if (path != null) {
            return world.file(path);
        }
        parent = world.locateClasspathItem(HostProperties.class).getParent();
        file = parent.join("host.properties");
        if (file.exists()) {
            return file;
        }
        file = world.getHome().join(".lavender/host.properties");
        if (file.exists()) {
            return file;
        }
        file = world.file("/etc/lavender/host.properties");
        if (file.exists()) {
            return file;
        }
        throw new IOException("cannot locate lavender properties");
    }

    private static final Separator SECRETS_PATH_SEPRATOR = Separator.on(':').trim();

    private static HostProperties properties(Node file) throws IOException, URISyntaxException {
        java.util.Properties properties;
        String str;
        FileNode cache;
        Secrets secrets;
        World world;
        Node network;
        HostProperties result;
        FileNode root;

        world = file.getWorld();
        properties = file.readProperties();
        str = eatOpt(properties,"cache", null);
        if (str == null) {
            cache = world.getHome().join(".cache/lavender");
        } else {
            cache = world.file(str);
        }
        secrets = new Secrets();
        str = eatOpt(properties, "secrets", null);
        if (str == null) {
            secrets.addAll(file.getParent().join("secrets.properties"));
        } else {
            for (String item : SECRETS_PATH_SEPRATOR.split(str)) {
                if (item.startsWith("/")) {
                    root = world.file("/");
                    item = item.substring(1);
                } else {
                    root = world.getHome();
                }
                for (FileNode match : root.find(item)) {
                    secrets.addAll(match);
                }
            }
        }
        str = eatOpt(properties, "network", null);
        if (str == null) {
            network = file.getParent().join("network.xml");
        } else {
            network = world.node(str);
        }
        result = new HostProperties(world, cache, network, secrets);
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("scm.")) {
                result.addScm(key.substring(4), new URI(eat(properties, key)));
            } else if (key.startsWith("ssh.")) {
                result.addSsh(file.getWorld().file(eat(properties, key)));
            }

        }
        if (!properties.isEmpty()) {
            throw new IOException("unknown properties: " + properties.keySet());
        }
        return result;
    }

    //--

    public final World world;
    private final FileNode cache;
    /** don't store the node, so I can create properties without accessing svn (and thus without svn credentials) */
    private final Map<String, URI> scms;
    public final Node network;
    public final Secrets secrets;
    private final List<Node> sshKeys;

    public HostProperties(World world, FileNode cache, Node network, Secrets secrets) {
        this.world = world;
        this.cache = cache;
        this.scms = new HashMap<>();
        this.network = network;
        this.secrets = secrets;
        this.sshKeys = new ArrayList<>();
    }

    public void addScm(String name, URI uri) throws IOException {
        if (scms.put(name, uri) != null) {
            throw new IOException("duplicate scm: " + uri);
        }
    }
    public void addSsh(FileNode key) {
        sshKeys.add(key);
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

    public URI getScm(String name) {
        return scms.get(name);
    }

    public FileNode cacheroot() throws IOException {
        cache.mkdirsOpt();
        return cache;
    }
}
