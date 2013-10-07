package net.oneandone.lavender.config;

import net.oneandone.lavender.index.Distributor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Target {
    public final Cluster cluster;
    public final Docroot docroot;
    public final Alias alias;

    public Target(Cluster cluster, Docroot docroot, Alias alias) {
        this.cluster = cluster;
        this.docroot = docroot;
        this.alias = alias;
    }

    public Distributor open(World world, String indexName) throws IOException {
        return Distributor.open(world, cluster.hosts(), docroot, indexName);
    }

    public void lockfiles(World world, List<Node> result) throws IOException {
        for (Host host : cluster.hosts()) {
            result.add(docroot.index(host.open(world), ".lock"));
        }
    }

    public List<Node> lock(World world, String user) throws IOException {
        return lock(world, user, Collections.singleton(this));
    }

    //--

    public static List<Node> lock(World world, String user, Set<Target> targets) throws IOException {
        List<Node> locks;

        locks = lockfiles(world, targets);
        for (Node file : locks) {
            try {
                file.mkfile().writeString(user);
            } catch (IOException e) {
                for (Node remove : locks) {
                    if (remove == file) {
                        break;
                    }
                    try {
                        remove.deleteFile();
                    } catch (IOException suppressed) {
                        e.addSuppressed(suppressed);
                    }
                }
                throw e;
            }
        }
        return locks;
    }

    public static void unlock(List<Node> locks) throws IOException {
        IOException e;

        e = new IOException("unlock failed");
        for (Node lock : locks) {
            try {
                lock.deleteFile();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
        }
        if (e.getSuppressed().length > 0) {
            throw e;
        }
    }

    public static List<Node> lockfiles(World world, Set<Target> targets) throws IOException {
        List<Node> files;

        files = new ArrayList<>();
        for (Target target : targets) {
            target.lockfiles(world, files);
        }
        return files;
    }
}
