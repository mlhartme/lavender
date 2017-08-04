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
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.Properties;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Globals {
    public final World world;
    public final Console console;
    public final boolean lastConfig;

    private final String user;
    private final boolean noLock;
    private final int await;

    private Net lazyNet;
    private Properties lazyProperties;

    public Globals(World world, Console console, boolean lastConfig, String user, boolean noLock, int await) {
        this(world, console, lastConfig, user, noLock, await, null, null);
    }

    public Globals(World world, Console console, boolean lastConfig, String user, boolean noLock, int await,
                   Properties properties, Net net) {
        this.world = world;
        this.console = console;
        this.lastConfig = lastConfig;
        this.noLock = noLock;
        this.user = user;
        this.await = await;

        this.lazyProperties = properties;
        this.lazyNet = net;
    }

    public Properties properties() throws IOException {
        if (lazyProperties == null) {
            lazyProperties = Properties.load(world);
        }
        return lazyProperties;
    }

    public FileNode lockedCache() throws IOException {
        return properties().lockedCache(await, user);
    }

    public Net net() throws IOException {
        Properties p;

        if (lazyNet == null) {
            p = properties();
            lazyNet = lastConfig ? p.loadLastNet() : p.loadNet();
        }
        return lazyNet;
    }

    public Pool pool() {
        return new Pool(world, noLock ? null : user, await);
    }
}
