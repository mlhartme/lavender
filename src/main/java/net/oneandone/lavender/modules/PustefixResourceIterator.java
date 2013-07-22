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

import net.oneandone.lavender.publisher.Resource;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class PustefixResourceIterator implements Iterator<Resource> {
    private final PustefixModule source;
    private final Node webapp;
    private List<Node> files;

    // iterating data

    private Resource next;

    private int nextFile;


    public static PustefixResourceIterator create(PustefixModule source, Node webapp) throws IOException {
        Filter filter;

        filter = webapp.getWorld().filter().include("**/*").predicate(Predicate.FILE);
        return new PustefixResourceIterator(source, webapp, webapp.find(filter));
    }


    public PustefixResourceIterator(PustefixModule source, Node webapp, List<Node> files) {
        this.source = source;
        this.webapp = webapp;
        this.files = files;
        this.nextFile = 0;
    }

    private static final String MODULES_PREFIX = "modules/";
    private static final int MODULES_PREFIX_LENGTH = MODULES_PREFIX.length();

    public boolean hasNext() {
        Node file;
        String path;

        if (next != null) {
            return true;
        }
        while (nextFile < files.size()) {
            file = files.get(nextFile++);
            path = file.getRelative(webapp);
            if (source.isPublicResource(path)) {
                String folder;
                int end;

                if (path.startsWith(MODULES_PREFIX) && ((end = path.indexOf('/', MODULES_PREFIX_LENGTH)) != -1)) {
                    folder = path.substring(MODULES_PREFIX_LENGTH, end);
                } else {
                    folder = source.getProjectName();
                }
                next = new Resource(file, path, folder);
                return true;
            }
        }
        return false;
    }

    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Resource result = next;
        next = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
