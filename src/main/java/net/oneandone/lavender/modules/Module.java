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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Label;

import java.io.IOException;

/**
 * Contains resources. Can iterate all resources and probe for existing ones.
 * Type describes what kind of resources it contains; you usually have many modules
 * in one application, and the type selects which of them you want to - e.g. - publish.
 * Current types are "web" and "flash". Modules have a descriptor the specifies the
 * type.
 */
public abstract class Module implements Iterable<Resource> {
    private final String type;
    private final String name;
    private final boolean lavendelize;

    /** Where to write resources when publishing. Used for flash publishing to add the application name. */
    private final String targetPathPrefix;

    public Module(String type, String name, boolean lavendelize, String targetPathPrefix) {
        this.type = type;
        this.name = name;
        this.lavendelize = lavendelize;
        this.targetPathPrefix = targetPathPrefix;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /** @return number of changed (updated or added) resources */
    public long publish(Distributor distributor) throws IOException {
        Label label;
        long count;

        count = 0;
        for (Resource resource : this) {
            if (lavendelize) {
                label = resource.labelLavendelized(targetPathPrefix, name);
            } else {
                label = resource.labelNormal(targetPathPrefix);
            }
            if (distributor.write(label, resource)) {
                count++;
            }
        }
        return count;
    }

    public abstract Resource probe(String path) throws IOException;

    public abstract void saveCaches() throws IOException;
}
