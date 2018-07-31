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

import net.oneandone.sushi.fs.OnShutdown;
import net.oneandone.sushi.fs.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages connections.
 */
public class Pool extends Thread implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Pool.class);

    public static Pool create(World world, String lockContent, int wait) {
        OnShutdown sushiShutdown;
        Pool result;

        sushiShutdown = world.onShutdown();
        result = new Pool(world, lockContent, wait, sushiShutdown);
        Runtime.getRuntime().addShutdownHook(result);
        Runtime.getRuntime().removeShutdownHook(sushiShutdown);
        return result;
    }

    private final World world;
    private final String lockContent;
    /** seconds to wait for a lock */
    private final int wait;
    private final List<Connection> connections;
    private final OnShutdown sushiShutdown;

    private Pool(World world, String lockContent, int wait, OnShutdown sushiShutdown) {
        this.world = world;
        this.lockContent = lockContent;
        this.wait = wait;
        this.connections = new ArrayList<>();
        this.sushiShutdown = sushiShutdown;
    }

    public Connection connect(Host host) throws IOException {
        Connection result;

        result = lookup(host);
        if (result == null) {
            result = host.connect(world, lockContent, wait);
            connections.add(result);
        }
        return result;
    }

    public Connection lookup(Host host) {
        for (Connection connection : connections) {
            if (host == connection.getHost()) {
                return connection;
            }
        }
        return null;
    }

    public void close() throws IOException {
        disconnect();

        // ok, no shutdown has occurred. Remove myself
        Runtime.getRuntime().removeShutdownHook(this);
        Runtime.getRuntime().addShutdownHook(sushiShutdown);
    }

    public void disconnect() throws IOException {
        IOException e;

        e = new IOException("cannot close connections");
        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
        }
        connections.clear();
        if (e.getSuppressed().length > 0) {
            throw e;
        }
    }

    /** shutdown handling */

    @Override
    public void run() {
        LOG.warn("shutdown hook on " + connections.size() + "connections triggered");
        try {
            disconnect();
        } catch (IOException e) {
            LOG.error("shutdown hook failed", e);
        }
        sushiShutdown.start();
        try {
            sushiShutdown.join();
        } catch (InterruptedException e) {
            LOG.error("shutdown hook interrupted");
        }
    }
}
