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

import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Where on a host to store files */
@Type
public class Docroot {
    @Value
    private String name;

    @Sequence(String.class)
    private List<String> domains;

    @Value
    private String documents;

    @Value
    private String indexes;

    public Docroot() {
        this(null, new ArrayList<>(), "", "");
    }

    public Docroot(String name, List<String> domains, String documents, String indexes) {
        if (name != null) {
            if (documents.startsWith("/") || documents.endsWith("/")) {
                throw new IllegalArgumentException(documents);
            }
            if (indexes.startsWith("/") || indexes.endsWith("/")) {
                throw new IllegalArgumentException(indexes);
            }
        }
        this.name = name;
        this.domains = domains;
        this.documents = documents;
        this.indexes = indexes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> domains() {
        return domains;
    }

    public String nodesFile() {
        StringBuilder builder;

        builder = new StringBuilder();
        for (String domain : domains) {
            if (domain.indexOf("://") < 0) {
                builder.append("http://").append(domain).append('\n');
                builder.append("https://").append(domain).append('\n');
            } else {
                builder.append(domain).append('\n');
            }
        }
        return builder.toString();
    }

    public String getDocuments() {
        return documents;
    }

    public void setDocuments(String documents) {
        this.documents = documents;
    }

    public String getIndexes() {
        return indexes;
    }

    public void setIndexes(String indexes) {
        this.indexes = indexes;
    }

    public Node node(Connection connection) {
        return connection.join(documents);
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
