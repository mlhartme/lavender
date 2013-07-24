package net.oneandone.lavender.config;

import java.util.HashMap;
import java.util.Map;

public class View {
    private final Map<String, Object[]> map;

    public View() {
        this.map = new HashMap<>();
    }

    public void add(String name, Cluster cluster, Docroot docroot, Alias alias) {
        map.put(name, new Object[] { cluster, docroot, alias });
    }
}
