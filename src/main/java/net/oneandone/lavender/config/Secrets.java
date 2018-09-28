/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.lavender.config;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.svn.SvnNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Secrets extends PropertiesBase {
    public static Secrets load(Node source) throws IOException {
        Properties p;
        Secrets result;

        p = source.readProperties();
        result = new Secrets();
        for (String name : names(p)) {
            if (p.remove(name + ".anonymous") != null) {
                result.add(name, null);
            } else {
                p.remove(name);
                result.add(name, new UsernamePassword(eat(p, name + ".username"), eat(p, name + ".password")));
            }
        }
        if (!p.isEmpty()) {
            throw new IOException("unknown keys: " + p.keySet());
        }
        return result;
    }

    private static List<String> names(Properties p) {
        List<String> names;
        Enumeration e;
        String name;

        names = new ArrayList<>();
        e = p.propertyNames();
        while (e.hasMoreElements()) {
            name = (String) e.nextElement();
            if (!name.contains(".")) {
                names.add(name);
            }
        }
        return names;
    }

    public Node withSecrets(Node orig) {
        if (orig instanceof SvnNode) {
            return orig;
        } else {
            return orig;
        }
    }

    //--

    private final Map<String, UsernamePassword> map;

    public Secrets() {
        this.map = new HashMap<>();
    }

    public void add(String name, UsernamePassword up) {
        map.put(name, up);
    }

    /**
     * @return credentials or null for anonymous
     * @throws IllegalStateException if credentials are missing
     */
    public UsernamePassword get(String url) throws NotFoundException {
        String best;

        best = null;
        for (String key : map.keySet()) {
            if (url.contains(key)) {
                if (best == null || (best.length() < key.length())) {
                    best = key;
                }
            }
        }
        if (best == null) {
            throw new NotFoundException(url);
        }
        return map.get(best);
    }

    public static class NotFoundException extends IOException {
        public NotFoundException(String url) {
            super(url);
        }
    }
}
