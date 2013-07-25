package net.oneandone.lavender.config;

import net.oneandone.lavender.index.Distributor;
import net.oneandone.sushi.fs.World;

import java.io.IOException;

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
        return Distributor.open(world, cluster.hosts, docroot, indexName);
    }

}
