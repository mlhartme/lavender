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
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.fs.svn.SvnFilesystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Settings {
    /** convenience method */
    public static Settings load() throws IOException {
        return load(file(new World()), true);
    }

    public static Settings load(Node file, boolean withSsh) throws IOException {
        Settings settings;

        settings = settings(file);
        settings.initWorld(withSsh);
        return settings;
    }

    public static Node file(World world) throws IOException {
        String path;
        Node file;
        FileNode dir;

        path = System.getenv("LAVENDER_SETTINGS");
        if (path != null) {
            file = world.file(path);
        } else {
            file = world.getHome().join(".lavender.settings");
            if (!file.exists()) {
                dir = world.locateClasspathItem(Settings.class).getParent();
                file = dir.join("lavender.settings");
                if (!file.exists()) {
                    throw new IOException("cannot locate lavender settings in " + world.getHome() + " or " + dir);
                }
            }
        }
        return file;
    }

    private static Settings settings(Node file) throws IOException {
        Properties properties;
        List<Node> sshKeys;

        properties = file.readProperties();
        sshKeys = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("ssh.")) {
                sshKeys.add(file.getWorld().file(properties.getProperty(key)));
            }
        }
        try {
            return new Settings(file.getWorld(), new URI(properties.getProperty("svn")), properties.getProperty("svn.username"), properties.getProperty("svn.password"), sshKeys);
        } catch (URISyntaxException e) {
            throw new IOException("invalid settings file " + file + ": " + e.getMessage(), e);
        }
    }

    //--

    public final World world;
    /** don't store the node, so I can create settings without accessing svn (and thus without svn credentials) */
    public final URI svn;
    public final String svnUsername;
    public final String svnPassword;
    private final List<Node> sshKeys;

    public Settings(World world, URI svn, String svnUsername, String svnPassword, List<Node> sshKeys) {
        this.world = world;
        this.svn = svn;
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.sshKeys = sshKeys;
    }

    public void initTemp(FileNode temp) throws IOException {
        FileNode parent;

        parent = temp.getParent();
        if (!parent.exists()) {
            parent.mkdir();
            parent.setPermissions("rwxrwxrwx");
        }
        world.setTemp((FileNode) temp.mkdirOpt());
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
        world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials(svnUsername, svnPassword);
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

    public Net loadNet() throws IOException {
        FileNode local;
        FileNode tmp;
        Net result;

        local = lastNetNode();
        tmp = local.getParent().createTempFile();
        world.node(svn).join("net.xml").copyFile(tmp);
        result = Net.load(tmp);
        tmp.move(local.deleteFileOpt());
        return result;
    }

    public Net loadLastNet() throws IOException {
        return Net.load(lastNetNode());
    }

    private FileNode lastNetNode() {
        return (FileNode) world.getHome().join(".lavender.net.xml");
    }
}
