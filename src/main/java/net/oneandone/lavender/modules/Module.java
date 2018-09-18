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

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains resources. Can iterate all resources and probe for existing ones. Resources originate from "entries", which are
 * loaded lazily.
 */
public abstract class Module<T> implements Iterable<Resource> {
    /** currently not used */
    public static final String TYPE = "web";

    private static final Logger LOG = LoggerFactory.getLogger(Module.class);

    /** currently not used; might be used again in the future if we want to publish different types to different clusters/docroots */
    private final String type;
    /** usually Maven coordinates without version; used to name md5 cache files */
    private final String name;
    private final boolean lavendelize;

    private final String resourcePathPrefix;

    /** Where to write resources when publishing. */
    private final String targetPathPrefix;

    private final Filter filter;

    /** maps resource names for module specific data for this resource; this data is typically used to instantiated resources */
    private Map<String, T> lazyEntries;

    private long lastScan;

    public Module(String type, String name, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        this.type = type;
        this.name = name;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.filter = filter;
        this.lazyEntries = null;
    }

    //--

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean getLavendeize() {
        return lavendelize;
    }

    public String getResourcePathPrefix() {
        return resourcePathPrefix;
    }

    public String getTargetPathPrefix() {
        return targetPathPrefix;
    }

    public Filter getFilter() {
        return filter;
    }

    //-- entry handling

    public Map<String, T> loadedEntries() {
        return lazyEntries;
    }

    /** invalidate entries if it's older than 5 seconds */
    public boolean softInvalidateEntries() {
        if (System.currentTimeMillis() - lastScan < 5000) {
            return false;
        } else {
            lazyEntries = null;
            return true;
        }
    }

    private Map<String, T> entries() throws IOException {
        long started;

        if (lazyEntries == null) {
            started = System.currentTimeMillis();
            try {
                lazyEntries = loadEntries();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(name + " entries failed: " + e.getMessage(), e);
            }
            LOG.info(name + ": scanned " + lazyEntries.size() + " names in " + (System.currentTimeMillis() - started) + "ms");
            lastScan = System.currentTimeMillis();
        }
        return lazyEntries;
    }

    /** @return all entries that match the module's filter */
    protected abstract Map<String, T> loadEntries() throws Exception;

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
        data = entries().get(path);
        return data == null ? null : createResource(resourcePath, data);
    }

    public Iterator<Resource> iterator() {
        final Iterator<Map.Entry<String, T>> base;

        try {
            base = entries().entrySet().iterator();
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

    public Label createLabel(Resource resource, byte[] md5) {
        String path;
        String targetPath;
        String filename;
        String md5str;

        path = resource.getPath();
        if (lavendelize) {
            filename = path.substring(path.lastIndexOf('/') + 1); // ok when not found
            md5str = Hex.encodeString(md5);
            if (md5str.length() < 3) {
                throw new IllegalArgumentException(md5str);
            }
            targetPath = targetPathPrefix + md5str.substring(0, 3) + "/" + md5str.substring(3) + "/" + name + "/" + filename;
        } else {
            targetPath = targetPathPrefix + path;
        }
        return new Label(path, targetPath, md5);
    }
}
