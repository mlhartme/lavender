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

import com.jcraft.jsch.JSchException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.fs.svn.SvnFilesystem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Settings {
    public static Settings loadAndInit(World world, String logs) throws IOException {
        Settings settings;

        settings = Settings.load(world);
        initWorld(world, settings, logs);
        return settings;
    }

    private static Settings load(World world) throws IOException {
        String path;
        Node file;
        Properties properties;
        List<Node> sshKeys;

        path = System.getenv("LAVENDER_SETTINGS");
        file = path != null ? world.file(path) : world.getHome().join(".lavender.settings");
        properties = file.readProperties();
        sshKeys = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("ssh.")) {
                sshKeys.add(world.file(properties.getProperty(key)));
            }
        }
        try {
            return new Settings(world.node(properties.getProperty("net")), properties.getProperty("svn.username"), properties.getProperty("svn.password"), sshKeys);
        } catch (URISyntaxException e) {
            throw new IOException("invalid settings file " + file + ": " + e.getMessage(), e);
        }
    }

    private static void initWorld(World world, Settings settings, String logspath) throws IOException {
        SshFilesystem ssh;
        FileNode logs;

        if (logspath == null) {
            logs = (FileNode) world.getHome().join("logs/lavender");
        } else {
            logs = world.file(logspath);
        }
        // /var/log is too small on pumamma64
        world.setTemp((FileNode) logs.mkdirOpt());

        world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials(settings.svnUsername, settings.svnPassword);
        ssh = world.getFilesystem("ssh", SshFilesystem.class);
        for (Node node : settings.sshKeys) {
            try {
                ssh.addIdentity(node, null);
            } catch (JSchException | IOException e) {
                throw new IllegalStateException("cannot connect to flash server: " + e.getMessage(), e);
            }
        }
        // disable them for integration tests, because I don't have .ssh on pearl/gems
    }

    //--

    public final Node net;
    public final String svnUsername;
    public final String svnPassword;
    public final List<Node> sshKeys;

    public Settings(Node net, String svnUsername, String svnPassword, List<Node> sshKeys) {
        this.net = net;
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.sshKeys = sshKeys;
    }
}
