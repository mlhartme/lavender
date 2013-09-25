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

import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class WarResourceIterator implements Iterator<Resource> {
    private final Node webapp;
    private List<Node> files;

    // iterating data

    private Resource next;

    private int nextFile;

    public WarResourceIterator(Node webapp, List<Node> files) {
        this.webapp = webapp;
        this.files = files;
        this.nextFile = 0;
    }

    public boolean hasNext() {
        Node file;
        String path;

        if (next != null) {
            return true;
        }
        while (nextFile < files.size()) {
            file = files.get(nextFile++);
            path = file.getRelative(webapp);
            try {
                next = DefaultResource.forNode(file, path);
            } catch (IOException e) {
                throw new RuntimeException("TODO", e);
            }
            return true;
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
