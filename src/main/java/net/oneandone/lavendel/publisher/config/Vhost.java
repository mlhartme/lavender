package net.oneandone.lavendel.publisher.config;

import java.util.ArrayList;
import java.util.List;

public class Vhost {
    public static Vhost one(String name) {
        List<String> domains;

        domains = new ArrayList<>();
        domains.add(name);
        return new Vhost(name, "", domains);
    }

    public final List<String> domains;

    public final String docroot;

    public final String name;

    public Vhost(String name, String docroot, List<String> domains) {
        this.name = name;
        this.docroot = docroot;
        this.domains = domains;
    }

    public String nodesFile() {
        StringBuilder builder;

        builder = new StringBuilder();
        for (String domain : domains) {
            builder.append("http://").append(domain).append('\n');
            builder.append("https://").append(domain).append('\n');
        }
        return builder.toString();
    }
}
