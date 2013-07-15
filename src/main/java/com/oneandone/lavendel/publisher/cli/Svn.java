package com.oneandone.lavendel.publisher.cli;

import com.oneandone.lavendel.index.Index;
import com.oneandone.lavendel.publisher.Distributor;
import com.oneandone.lavendel.publisher.Log;
import com.oneandone.lavendel.publisher.config.Cluster;
import com.oneandone.lavendel.publisher.config.Filter;
import com.oneandone.lavendel.publisher.config.Net;
import com.oneandone.lavendel.publisher.config.Vhost;
import com.oneandone.lavendel.publisher.svn.SvnExtractor;
import com.oneandone.lavendel.publisher.svn.SvnExtractorConfig;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;

import java.io.IOException;

public class Svn extends Base {
    @Value(name = "cluster", position = 1)
    private String clusterName;

    @Value(name = "directory", position = 2)
    private String directory;

    public Svn(Console console, Net net) {
        super(console, net);
    }

    @Override
    public void invoke() throws IOException {
        String url;

        url = net.svn.get(directory);
        if (url == null) {
            throw new ArgumentException("unknown svn directory: " + directory);
        }
        invoke(url);
    }

    private void invoke(String svnurl) throws IOException {
        Cluster cluster;
        Vhost vhost;
        Filter filter;
        SvnExtractorConfig ec;
        SvnExtractor e;
        Log log;
        Distributor storage;
        long changed;
        Index index;

        cluster = net.cluster(clusterName);
        vhost = cluster.vhost("svn");
        log = War.createLog(console);
        filter = new Filter();
        filter.setIncludes("*");
        filter.setExcludes();
        ec = new SvnExtractorConfig("svn", filter);
        ec.pathPrefix = "";
        ec.svn = svnurl;
        ec.lavendelize = false;
        e = ec.create(console.world, log);
        storage = Distributor.open(console.world, vhost.docroot, cluster.hosts, directory + ".idx");
        changed = e.run(storage);
        index = storage.close();
        console.info.println("done: " + changed + "/" + index.size() + " files changed");
    }
}
