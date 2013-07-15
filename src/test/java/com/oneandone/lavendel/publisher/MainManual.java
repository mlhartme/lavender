package com.oneandone.lavendel.publisher;

import com.oneandone.lavendel.publisher.cli.Main;
import com.oneandone.lavendel.publisher.config.Cluster;
import com.oneandone.lavendel.publisher.config.Host;
import com.oneandone.lavendel.publisher.config.Net;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import org.junit.Test;

public class MainManual {
    @Test
    public void sshAccess() throws Exception {
        Net net;
        World world;
        Node node;

        net = Net.normal();
        world = Main.world(true, null);
        for (Cluster cluster : net.clusters.values()) {
            for (Host host : cluster.hosts) {
                node = host.docroot(host.open(world), "");
                System.out.println(host + ":\n  " + node.list());
            }
        }
    }
}
