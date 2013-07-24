package net.oneandone.lavender.config;

/**
* Created with IntelliJ IDEA.
* User: mhm
* Date: 24.07.13
* Time: 16:00
* To change this template use File | Settings | File Templates.
*/
public class Target {
    public final Cluster cluster;
    public final Docroot docroot;
    public final Alias alias;

    public Target(Cluster cluster, Docroot docroot, Alias alias) {
        this.cluster = cluster;
        this.docroot = docroot;
        this.alias = alias;
    }
}
