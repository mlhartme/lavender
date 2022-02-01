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

import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IndexedModule extends Module<IndexedEntry> {
    private final String accessPathPrefix;
    private final PustefixJarConfig configOpt;
    private final Map<String, String> index; // maps path to md5
    private final ScmRoot scmRoot;

    // CHECKSTYLE:OFF
    public IndexedModule(String origin, String name, ModuleProperties descriptorOpt, boolean lavendelize,
                         String resourcePathPrefix, String targetPathPrefix, Filter filter,
                         String accessPathPrefix, PustefixJarConfig configOpt, Map<String, String> index, ScmRoot scmRoot) {
        super(origin, name, descriptorOpt, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
        this.accessPathPrefix = accessPathPrefix;
        this.configOpt = configOpt;
        this.index = index;
        this.scmRoot = scmRoot;
    }
    // CHECKSTYLE:ON

    @Override
    protected Map<String, IndexedEntry> loadEntries() throws IOException {
        Map<String, IndexedEntry> result;
        String path;
        String publicPath;

        result = new HashMap<>();
        for (Map.Entry<String, String> entry : index.entrySet()) {
            path = entry.getKey();
            if (configOpt != null) {
                publicPath = configOpt.getPath(path);
            } else {
                publicPath = path;
            }
            if (publicPath != null) {
                result.put(publicPath, new IndexedEntry(publicPath, accessPathPrefix + path, entry.getValue()));
            }
        }
        return result;
    }

    @Override
    protected Resource createResource(String resourcePath, IndexedEntry entry) {
        return new IndexedResource(scmRoot, resourcePath, entry.accessPath, entry.md5);
    }
}
