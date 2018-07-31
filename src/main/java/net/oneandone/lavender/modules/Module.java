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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /** currently not used */
    public static final String TYPE = "web";

    private static final Logger LOG = LoggerFactory.getLogger(Module.class);

    private final String type;
    private final String name;
    private final boolean lavendelize;

    private final String resourcePathPrefix;

    /** Where to write resources when publishing. Used for flash publishing to add the application name. */
    private final String targetPathPrefix;

    private final Filter filter;

    private Map<String, T> files;

    private long lastScan;

    public Module(String type, String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        this.type = type;
        this.name = name;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.filter = filter;
        this.files = null;
    }

    public String getResourcePathPrefix() {
        return resourcePathPrefix;
    }

    /** currently not used; might be used again in the future if we want to publish different types do different clusters/docroots */
    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean hasFiles() {
        return files != null;
    }

    private Map<String, T> files() throws IOException {
        long started;

        if (files == null) {
            started = System.currentTimeMillis();
            try {
                files = scan(filter);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(name + " scan failed: " + e.getMessage(), e);
            }
            LOG.info(name + ": scanned " + files.size() + " files in " + (System.currentTimeMillis() - started) + "ms");
            lastScan = System.currentTimeMillis();
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

    public String matches(String resourcePath) throws IOException {
        String path;

        if (!resourcePath.startsWith(resourcePathPrefix)) {
            return null;
        }
        path = resourcePath.substring(resourcePathPrefix.length());
        if (!filter.matches(path)) {
            return null;
        }
        return path;
    }

    public Resource probe(String resourcePath) throws IOException {
        String path;
        T file;

        path = matches(resourcePath);
        if (path == null) {
            return null;
        }
        file = files().get(path);
        return file == null ? null : createResource(resourcePath, file);
    }

    public boolean softInvalidate() throws IOException {
        if (System.currentTimeMillis() - lastScan < 5000) {
            return false;
        } else {
            files = null;
            return true;
        }
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
    protected abstract Map<String, T> scan(Filter filer) throws Exception;

    protected abstract Resource createResource(String path, T file) throws IOException;

    public abstract void saveCaches() throws IOException;
}
