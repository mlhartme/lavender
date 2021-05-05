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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.util.Map;

public class IndexedModule extends Module<String> {
    private final World world;
    private final Map<String, String> index; // maps path to md5
    private final String urlPattern; // with variables ${tag} and ${path}

    // CHECKSTYLE:OFF
    public IndexedModule(World world, String origin, String name, ScmProperties descriptorOpt, boolean lavendelize,
                         String resourcePathPrefix, String targetPathPrefix, Filter filter,
                         Map<String, String> index, String urlPattern) {
        super(origin, name, descriptorOpt, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
        this.world = world;
        this.index = index;
        this.urlPattern = urlPattern;
    }
    // CHECKSTYLE:ON

    @Override
    protected Map<String, String> loadEntries() throws IOException {
        return index;
    }

    @Override
    protected Resource createResource(String resourcePath, String entry) {
        return new IndexedResource(world, urlPattern, resourcePath, entry);
    }


}
