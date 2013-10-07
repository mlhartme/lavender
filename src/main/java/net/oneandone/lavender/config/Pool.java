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
    private final String lock;
    private final List<Connection> connections;

    public Pool(World world, String lock) {
        this.world = world;
        this.lock = lock;
        this.connections = new ArrayList<>();
    }

    public Connection connect(Host host) throws IOException {
        Connection result;

        result = lookup(host);
        if (result == null) {
            result = host.connect(world, lock);
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
