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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Secrets extends PropertiesBase {
    private static final Logger LOG = LoggerFactory.getLogger(Secrets.class);

    private final Map<String, UsernamePassword> map;

    public Secrets() {
        this.map = new HashMap<>();
    }

    public void add(String name, UsernamePassword up) throws IOException {
        if (map.put(name, up) != null) {
            throw new IOException("duplicate secrets: " + name);
        }
    }

    public void addAll(Node source) throws IOException {
        LOG.debug("addAll secrets: " + source);

        Properties p;

        p = source.readProperties();
        for (String name : names(p)) {
            if (p.remove(name + ".anonymous") != null) {
                add(name, UsernamePassword.ANONYMOUS);
            } else {
                p.remove(name);
                add(name, new UsernamePassword(eat(p, name + ".username"), eat(p, name + ".password")));
            }
        }
        if (!p.isEmpty()) {
            throw new IOException("unknown keys: " + p.keySet());
        }
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

    public UsernamePassword get(String url) throws NotFoundException, AmbiguousException {
        UsernamePassword result;

        result = lookup(url);
        if (result == null) {
            throw new NotFoundException(url);
        }
        return result;
    }

    /**
     * @return null for not found
     */
    public UsernamePassword lookup(String url) throws AmbiguousException {
        String best;

        best = null;
        for (String key : map.keySet()) {
            if (url.contains(key)) {
                if (best == null || (best.length() < key.length())) {
                    best = key;
                } else if (best.length() == key.length()) {
                    throw new AmbiguousException(best + " vs " + key);
                }
            }
        }
        return best == null ? null : map.get(best);
    }

    public URI withSecrets(URI uri) throws IOException {
        if (uri.isOpaque()) {
            // e.g. svn, git, ...
            return get(uri.toString()).add(uri);
        } else {
            return uri;
        }
    }

    //--

    public static class AmbiguousException extends IOException {
        public AmbiguousException(String str) {
            super(str);
        }
    }
    public static class NotFoundException extends IOException {
        public NotFoundException(String str) {
            super("username/password not found in secrets: " + str);
        }
    }
}
