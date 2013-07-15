package com.oneandone.lavendel.publisher.config;

import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.HashMap;
import java.util.Map;

public class Net {
    public static Net normal() {
        Net net;

        net = new Net();
        net.add("eu", new Cluster()
            .addCdn("cdnfe01.schlund.de")
            .addCdn("cdnfe02.schlund.de")
            .addCdn("cdnfe03.schlund.de")
            .addVhost("s", "/fix", "s1.uicdn.net", "s2.uicdn.net", "s3.uicdn.net", "s4.uicdn.net"));
        net.add("us", new Cluster()
                // see http://issue.tool.1and1.com/browse/ITOSHA-3624 and http://issue.tool.1and1.com/browse/ITOSHA-3667
                .addCdn("wscdnfelxaa01.fe.server.lan")
                .addCdn("wscdnfelxaa02.fe.server.lan")
                .addCdn("wscdnfelxaa03.fe.server.lan")
                .addVhost("u", "/fix", "u1.uicdn.net", "u2.uicdn.net", "u3.uicdn.net", "u4.uicdn.net")
                .addVhost("au", "/fix", "au1.uicdn.net", "au2.uicdn.net", "au3.uicdn.net", "au4.uicdn.net"));
        net.add("walter", new Cluster()
                .addHost("walter.websales.united.domain", "mhm", "Users/mhm/lavendel/htdocs", "Users/mhm/lavendel/indexes")
                .addVhost("fix", "/fix", "fix.lavendel.walter.websales.united.domain")
                .addVhost("svn", "/var", "var.lavendel.walter.websales.united.domain"));
        net.add("bazaar", new Cluster()
                .addStatint("cdnfe01.schlund.de")
                .addStatint("cdnfe02.schlund.de")
                .addStatint("cdnfe03.schlund.de"));
        net.add("flash-eu", new Cluster()
                // see http://issue.tool.1and1.com/browse/ITOSHA-3624 and http://issue.tool.1and1.com/browse/ITOSHA-3668
                .addFlash("winflasheu1.schlund.de")
                .addFlash("winflasheu2.schlund.de")
                .addVhost("flash", "", "notused"));
        net.add("flash-us", new Cluster()
                .addFlash("winflashus1.lxa.perfora.net")
                .addFlash("winflashus2.lxa.perfora.net")
                .addVhost("flash", "", "notused"));
        net.addSvn("downloads", "https://svn.1and1.org/svn/PFX/lavendel/cors");
        return net;
    }

    //--

    public static Host local(FileNode docrootbase, FileNode index) {
        return new Host(null, null, docrootbase.getPath(), index.getPath());
    }

    //--

    public final Map<String, Cluster> clusters;

    public final Map<String, String> svn;

    public Net() {
        clusters = new HashMap<>();
        svn = new HashMap<>();
    }

    public void addSvn(String name, String url) {
        svn.put(name, url);
    }

    public void add(String name, Cluster cluster) {
        if (clusters.put(name, cluster) != null) {
            throw new IllegalArgumentException("duplicate cluster: " + name);
        }
    }

    public Cluster cluster(String name) {
        Cluster result;

        result = clusters.get(name);
        if (result == null) {
            throw new ArgumentException("unknown cluster: " + name);
        }
        return result;
    }
}
