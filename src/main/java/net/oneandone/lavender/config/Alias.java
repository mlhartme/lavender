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
package net.oneandone.lavender.config;

import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Lists the domains that point to the respective docroot. And a name to refer to this set of domains. */
@Type
public class Alias {
    public static Alias one(String name) {
        return new Alias(name, name);
    }

    @Value
    private String name;

    @Sequence(String.class)
    private List<String> domains;

    public Alias() {
        this("dummy");
    }

    public Alias(String name, String ... domains) {
        this(name, new ArrayList<>(Arrays.asList(domains)));
    }

    public Alias(String name, List<String> domains) {
        this.name = name;
        this.domains = domains;
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
}
