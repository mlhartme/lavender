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

import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains resources. Can iterate all resources and probe for existing ones. Resources orginate from "entries", that
 * are loaded lazyly.
 */
public abstract class Module<T> implements Iterable<Resource> {
    /** currently not used */
    public static final String TYPE = "web";

    private static final Logger LOG = LoggerFactory.getLogger(Module.class);

    /** currently not used; might be used again in the future if we want to publish different types do different clusters/docroots */
    private final String type;
    private final String name;
    private final boolean lavendelize;

    private final String resourcePathPrefix;

    /** Where to write resources when publishing. Used for flash publishing to add the application name. */
    private final String targetPathPrefix;

    private final Filter filter;

    /** maps resource names for module specific data for this resource; this data is typically used to instantiated resources */
    private Map<String, T> lazyScan;

    private long lastScan;

    public Module(String type, String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        this.type = type;
        this.name = name;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.filter = filter;
        this.lazyScan = null;
    }

    //--

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getResourcePathPrefix() {
        return resourcePathPrefix;
    }

    public boolean getLavendeize() {
        return lavendelize;
    }

    public String getTargetPathPrefix() {
        return targetPathPrefix;
    }

    //-- scans

    public boolean hasScan() {
        return lazyScan != null;
    }

    /** invalidate scan if it's older than 5 seconds */
    public boolean softInvalidateScan() {
        if (System.currentTimeMillis() - lastScan < 5000) {
            return false;
        } else {
            lazyScan = null;
            return true;
        }
    }

    private Map<String, T> scan() throws IOException {
        long started;

        if (lazyScan == null) {
            started = System.currentTimeMillis();
            try {
                lazyScan = doScan(filter);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(name + " scan failed: " + e.getMessage(), e);
            }
            LOG.info(name + ": scanned " + lazyScan.size() + " names in " + (System.currentTimeMillis() - started) + "ms");
            lastScan = System.currentTimeMillis();
        }
        return lazyScan;
    }

    public String matches(String resourcePath) {
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

    /** do scan for resource names and possibly data to speedup resource creation */
    protected abstract Map<String, T> doScan(Filter filter) throws Exception;

    //-- resources

    protected abstract Resource createResource(String path, T data) throws IOException;

    /** @return null if not found */
    public Resource probe(String resourcePath) throws IOException {
        String path;
        T data;

        path = matches(resourcePath);
        if (path == null) {
            return null;
        }
        data = scan().get(path);
        return data == null ? null : createResource(resourcePath, data);
    }

    public Iterator<Resource> iterator() {
        final Iterator<Map.Entry<String, T>> base;

        try {
            base = scan().entrySet().iterator();
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

    public abstract void saveCaches() throws IOException;

    public Label createLabel(Resource resource, byte[] md5) {
        return lavendelize ? resource.labelLavendelized(targetPathPrefix, name, md5) : resource.labelNormal(targetPathPrefix, md5);
    }
}
