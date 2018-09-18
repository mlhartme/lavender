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
package net.oneandone.lavender.cli;

import net.oneandone.inline.Console;
import net.oneandone.lavender.config.Network;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.SystemProperties;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Globals {
    public final World world;
    public final Console console;
    public final Main.Commandline commandline;
    public final boolean lastConfig;

    private final String user;
    private final boolean noLock;
    private final int await;

    private Network lazyNet;
    private SystemProperties lazyProperties;

    public Globals(World world, Console console, Main.Commandline commandline, boolean lastConfig, String user, boolean noLock, int await) {
        this(world, console, commandline, lastConfig, user, noLock, await, null, null);
    }

    public Globals(World world, Console console, Main.Commandline commandline, boolean lastConfig, String user, boolean noLock, int await,
                   SystemProperties properties, Network net) {
        this.world = world;
        this.console = console;
        this.commandline = commandline;
        this.lastConfig = lastConfig;
        this.noLock = noLock;
        this.user = user;
        this.await = await;

        this.lazyProperties = properties;
        this.lazyNet = net;
    }

    public SystemProperties properties() throws IOException {
        if (lazyProperties == null) {
            lazyProperties = SystemProperties.load(world);
        }
        return lazyProperties;
    }

    public FileNode cacheroot() throws IOException {
        return properties().cacheroot();
    }

    public Network net() throws IOException {
        SystemProperties p;

        if (lazyNet == null) {
            p = properties();
            lazyNet = lastConfig ? p.loadLastNetwork() : p.loadNetwork();
        }
        return lazyNet;
    }

    public Pool pool() {
        return Pool.create(world, noLock ? null : lockInfo(), await);
    }

    private String lockInfo() {
        String hostname;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknownHostname";
        }

        return user + '@' + hostname + ": " + Separator.SPACE.join(commandline.args);
    }
}
