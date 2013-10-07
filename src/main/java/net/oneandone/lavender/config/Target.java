package net.oneandone.lavender.config;

import net.oneandone.lavender.index.Distributor;

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

    public Distributor open(Pool pool, String indexName) throws IOException {
        return Distributor.open(cluster.connect(pool), docroot, indexName);
    }
}
