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

import net.oneandone.lavender.config.Filter;
import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Label;

import java.io.IOException;

/**
 * Contains resources. Can iterate all resources and probe for existing ones.
 * Type describes what kind of resources it contains; you usually have many modules
 * in one application, and the type selects which of the you want to - e.g. - publish.
 * Current types are "web" and "flash". Modules have a descriptor the specifies the
 * type.
 */
public abstract class Module implements Iterable<Resource> {
    private final Filter filter;
    private final String type;
    private final String folder;
    private final boolean lavendelize;
    private final String pathPrefix;

    public Module(Filter filter, String type, String folder, boolean lavendelize, String pathPrefix) {
        if (filter == null) {
            throw new IllegalArgumentException();
        }
        this.filter = filter;
        this.type = type;
        this.folder = folder;
        this.lavendelize = lavendelize;
        this.pathPrefix = pathPrefix;
    }

    public String getType() {
        return type;
    }

    public Filter getFilter() {
        return filter;
    }

    /** @return number of changed (updated or added) resources */
    public long publish(Distributor distributor) throws IOException {
        Label label;
        long count;

        count = 0;
        for (Resource resource : this) {
            if (filter.isIncluded(resource.getPath())) {
                if (lavendelize) {
                    label = resource.labelLavendelized(pathPrefix, folder);
                } else {
                    label = resource.labelNormal(pathPrefix);
                }
                if (distributor.write(label, resource)) {
                    count++;
                }
            }
        }
        return count;
    }

    public Resource probe(String path) throws IOException {
        return filter.isIncluded(path) ? probeIncluded(path) : null;
    }

    protected abstract Resource probeIncluded(String path) throws IOException;

    public abstract void saveCaches() throws IOException;
}
