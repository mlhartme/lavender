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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.io.OutputStream;

public class IndexedResource extends Resource {
    private final World world;
    private final String urlPattern;
    private final String resourcePath;
    private final String md5;

    public IndexedResource(World world, String urlPattern, String resourcePath, String md5) {
        this.world = world;
        this.urlPattern = urlPattern;
        this.resourcePath = resourcePath;
        this.md5 = md5;
    }

    @Override
    public String getPath() {
        return resourcePath;
    }

    @Override
    public String getContentId() {
        return md5;
    }

    @Override
    public String getOrigin() {
        return lazyUrl.getUri().toString();
    }

    @Override
    public void writeTo(OutputStream dest) throws IOException {
        url().copyFileTo(dest);
    }

    private Node<?> lazyUrl = null;

    private Node<?> url() throws NodeInstantiationException {
        if (lazyUrl == null) {
            lazyUrl = world.validNode(urlPattern.replace("$(path}", resourcePath));
        }
        return lazyUrl;
    }

    @Override
    public boolean isOutdated() {
        // TODO
        return true;
    }
}
