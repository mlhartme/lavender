package net.oneandone.lavender.config;

import net.oneandone.sushi.cli.ArgumentException;

import java.util.HashMap;
import java.util.Map;

public class View {

    private final Map<String, Target> map;

    public View() {
        this.map = new HashMap<>();
    }

    public void add(String name, Cluster cluster, Docroot docroot, Alias alias) {
        map.put(name, new Target(cluster, docroot, alias));
    }

    public Target get(String name) {
        Target target;

        target = map.get(name);
        if (target == null) {
            throw new ArgumentException("no such target: " + target);
        }
        return target;
    }
}
