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

import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages connections.
 */
public class Pool implements AutoCloseable {
    private final World world;
    private final String lockContent;
    /** seconds to wait for a lock */
    private final int wait;
    private final List<Connection> connections;

    public Pool(World world, String lockContent, int wait) {
        this.world = world;
        this.lockContent = lockContent;
        this.wait = wait;
        this.connections = new ArrayList<>();
    }

    public Connection connect(Host host, String lockPath) throws IOException {
        Connection result;

        result = lookup(host);
        if (result == null) {
            result = host.connect(world, lockPath, lockContent, wait);
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
        IOException e;

        e = new IOException("cannot close connections");
        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
        }
        if (e.getSuppressed().length > 0) {
            throw e;
        }
    }
}
