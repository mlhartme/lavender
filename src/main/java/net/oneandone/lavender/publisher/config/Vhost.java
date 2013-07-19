/**
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
package net.oneandone.lavender.publisher.config;

import java.util.ArrayList;
import java.util.List;

/** multiple vhost may share the same docroot. */
public class Vhost {
    public static Vhost one(String name) {
        List<String> domains;

        domains = new ArrayList<>();
        domains.add(name);
        return new Vhost(name, "", domains);
    }

    public final String name;

    public final String docroot;

    public final List<String> domains;

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
