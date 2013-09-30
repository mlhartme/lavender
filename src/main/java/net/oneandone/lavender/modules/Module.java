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
import org.apache.log4j.spi.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains resources. Can iterate all resources and probe for existing ones.
 * Type describes what kind of resources it contains; you usually have many modules
 * in one application, and the type selects which of them you want to - e.g. - publish.
 * Current types are "web" and "flash". Modules have a descriptor the specifies the
 * type.
 */
public abstract class Module<T> implements Iterable<Resource> {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Module.class);

    private final String type;
    private final String name;
    private final boolean lavendelize;

    private final String resourcePathPrefix;

    /** Where to write resources when publishing. Used for flash publishing to add the application name. */
    private final String targetPathPrefix;

    private Map<String, T> files;

    public Module(String type, String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix) {
        this.type = type;
        this.name = name;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.files = null;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    private Map<String, T> files() throws IOException {
        long started;

        if (files == null) {
            started = System.currentTimeMillis();
            files = scan();
            LOG.info(name + ": scanned " + files.size() + " files in " + (System.currentTimeMillis() - started) + "ms");
        }
        return files;
    }

    public Iterator<Resource> iterator() {
        final Iterator<Map.Entry<String, T>> base;

        try {
            base = files().entrySet().iterator();
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
        return new Iterator<Resource>() {
            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public Resource next() {
                Map.Entry<String, T> entry;

                entry = base.next();
                try {
                    return createResource(resourcePathPrefix + entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    throw new RuntimeException("TODO", e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Resource probe(String resourcePath) throws IOException {
        String path;
        Resource fromCache;
        T file;

        if (!resourcePath.startsWith(resourcePathPrefix)) {
            return null;
        }
        path = resourcePath.substring(resourcePathPrefix.length());
        if (files != null) {
            file = files.get(path);
            fromCache = file == null ? null : createResource(resourcePath, file);
            if (fromCache != null && !fromCache.isOutdated()) {
                return fromCache;
            }
            // invalidate cache
            files = null;
        }
        file = files().get(path);
        return file == null ? null : createResource(resourcePath, file);
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

    //--

    /** scan for files in this module */
    protected abstract Map<String, T> scan() throws IOException;

    protected abstract Resource createResource(String path, T file) throws IOException;

    public abstract void saveCaches() throws IOException;
}
