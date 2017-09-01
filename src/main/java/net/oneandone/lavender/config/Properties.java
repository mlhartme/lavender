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

import net.oneandone.lavender.modules.SvnModule;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.fs.svn.SvnFilesystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Properties {
    private static final Logger LOG = LoggerFactory.getLogger(Properties.class);

    public static Properties load(World world) throws IOException {
        return load(file(world), true);
    }

    public static Properties load(Node file, boolean withSsh) throws IOException {
        Properties properties;

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
        parent = world.locateClasspathItem(Properties.class).getParent();
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

    private static Properties properties(Node file) throws IOException {
        java.util.Properties properties;
        List<Node> sshKeys;
        String cache;
        FileNode cacheNode;

        properties = file.readProperties();
        sshKeys = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("ssh.")) {
                sshKeys.add(file.getWorld().file(properties.getProperty(key)));
            }
        }
        cache = properties.getProperty("cache");
        if (cache == null) {
            cacheNode = (FileNode) file.getWorld().getHome().join(".cache/lavender");
        } else {
            cacheNode = file.getWorld().file(cache);
        }
        try {
            return new Properties(file.getWorld(), cacheNode,
                    new URI(properties.getProperty("svn")), properties.getProperty("svn.username"), properties.getProperty("svn.password"), sshKeys);
        } catch (URISyntaxException e) {
            throw new IOException("invalid properties file " + file + ": " + e.getMessage(), e);
        }
    }

    //--

    public final World world;
    private final FileNode cache;
    /** don't store the node, so I can create properties without accessing svn (and thus without svn credentials) */
    public final URI svn;
    public final String svnUsername;
    public final String svnPassword;
    private final List<Node> sshKeys;

    public Properties(World world, FileNode cache, URI svn, String svnUsername, String svnPassword, List<Node> sshKeys) {
        this.world = world;
        this.cache = cache;
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
        tmp = SvnModule.newTmpFile(local.getParent());
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

    public FileNode lockedCache(int wait, String lockContent) throws IOException {
        cache.mkdirsOpt();
        doLock(wait, lockContent);
        return cache;
    }

    private void doLock(int wait, String lockContent) throws IOException {
        FileNode lock;
        int seconds;

        lock = cacheLock();
        seconds = 0;
        while (true) {
            try {
                lock.mkfile();
                break;
            } catch (IOException e) {
                seconds++;
                if (seconds >= wait) {
                    throw new IOException("cannot create " + lock);
                }
                if (seconds % 10 == 0) {
                    LOG.info("waiting for cache-lock " + lock + ", seconds=" + seconds);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // fall-through
                }
            }
        }
        lock.writeString(lockContent);
    }

    private FileNode cacheLock() {
        return cache.join(".lock");
    }

    public void unlockCache() throws IOException {
        cacheLock().deleteFile();
    }
}
