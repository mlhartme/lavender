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

import net.oneandone.inline.ArgumentException;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** Where on a host to store files */
@Type
public class Docroot {
    public static final String WEB = "web";
    public static final String SVN = "svn";

    @Value
    private String name;

    @Value
    private String docroot;

    @Value
    private String indexes;

    @Sequence(Alias.class)
    private final List<Alias> aliases;

    public Docroot() {
        this(null, "", "");
    }

    public Docroot(String name, String docroot, String indexes, Alias... aliases) {
        this(name, docroot, indexes, new ArrayList<>(Arrays.asList(aliases)));
    }

    public Docroot(String name, String docroot, String indexes, List<Alias> aliases) {
        if (name != null) {
            if (aliases.isEmpty()) {
                throw new IllegalArgumentException("missing alias for docroot " + docroot);
            }
            if (docroot.startsWith("/") || docroot.endsWith("/")) {
                throw new IllegalArgumentException(docroot);
            }
            if (indexes.startsWith("/") || indexes.endsWith("/")) {
                throw new IllegalArgumentException(indexes);
            }
        }
        this.name = name;
        this.docroot = docroot;
        this.indexes = indexes;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocroot() {
        return docroot;
    }

    public void setDocroot(String docroot) {
        this.docroot = docroot;
    }

    public String getIndexes() {
        return indexes;
    }

    public void setIndexes(String indexes) {
        this.indexes = indexes;
    }

    public List<Alias> aliases() {
        return aliases;
    }


    public Alias alias(String name) {
        for (Alias alias : aliases) {
            if (name.equals(alias.getName())) {
                return alias;
            }
        }
        throw new ArgumentException("alias not found: " + name);
    }

    public Node node(Connection connection) {
        return connection.join(docroot);
    }

    public Node index(Connection connection, String indexName) {
        return connection.join(indexes, indexName);
    }

    /** @return all indexes without the all index */
    public List<? extends Node> indexList(Connection connection) throws ListException, DirectoryNotFoundException {
        List<? extends Node> result;
        Iterator<? extends Node> iter;

        result = connection.join(indexes).list();
        iter = result.iterator();
        while (iter.hasNext()) {
            if (iter.next().getName().startsWith(".")) {
                iter.remove();
            }
        }
        return result;
    }
}
