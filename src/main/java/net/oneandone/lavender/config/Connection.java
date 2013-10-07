package net.oneandone.lavender.config;

import net.oneandone.sushi.fs.Node;

import java.io.IOException;

/** Node and optionally lock on a host. */
public class Connection implements AutoCloseable {
    public static Connection openLocked(Host host, Node root, String lock) throws IOException {
        Node lockfile;

        lockfile = root.join("tmp/lavender.lock");
        lockfile.mkfile();
        lockfile.writeString(lock);
        return new Connection(host, root, lockfile);
    }

    public static Connection openSimple(Host host, Node root) {
        return new Connection(host, root, null);
    }

    //--

    private final Host host;
    private final Node root;
    private final Node lock;

    public Connection(Host host, Node root, Node lock) {
        this.host = host;
        this.root = root;
        this.lock = lock;
    }

    public Host getHost() {
        return host;
    }

    public Node join(String ... names) {
        return root.join(names);
    }

    public void close() throws IOException {
        if (lock != null) {
            lock.deleteFile();
        }
    }
}
