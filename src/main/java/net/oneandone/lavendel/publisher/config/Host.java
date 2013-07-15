package net.oneandone.lavendel.publisher.config;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

public class Host {
    /** null for localhost (login is ignored in this case, and open returns FileNodes) */
    public final String name;
    public final String login;

    /** where docroots of the various domains reside */
    private final String docrootbase;
    private final String indexbase;

    public Host(String name, String login, String docrootbase, String indexbase) {
        if (docrootbase.startsWith("/") || docrootbase.endsWith("/")) {
            throw new IllegalArgumentException(docrootbase);
        }
        if (indexbase.startsWith("/") || indexbase.endsWith("/")) {
            throw new IllegalArgumentException(indexbase);
        }
        this.name = name;
        this.login = login;
        this.docrootbase = docrootbase;
        this.indexbase = indexbase;
    }

    /** @return root node of this host */
    public Node open(World world) throws NodeInstantiationException {
        if (name != null) {
            return world.validNode("ssh://" + login + "@" + name);
        } else {
            return world.file("/");
        }
    }

    public Node docroot(Node root, String suffix) throws NodeInstantiationException {
        return root.join(docrootbase + suffix);
    }

    public Node index(Node root, String indexName) throws NodeInstantiationException {
        return root.join(indexbase, indexName);
    }

    public String toString() {
        if (name == null) {
            return "[localhost]";
        } else {
            return "[" + login + "@" + name + "]";
        }
    }
}
