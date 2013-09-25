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
import java.util.Map;
import java.util.NoSuchElementException;

public class JarResourceIterator implements Iterator<Resource> {
    private final Iterator<Map.Entry<String, Node>> files;
    private Resource next;

    public JarResourceIterator(Iterator<Map.Entry<String, Node>> files) {
        this.files = files;
    }

    public boolean hasNext() {
        Map.Entry<String, Node> entry;

        if (next != null) {
            return true;
        }
        if (files.hasNext()) {
            entry = files.next();
            try {
                next = DefaultResource.forNode(entry.getValue(), entry.getKey());
            } catch (IOException e) {
                throw new RuntimeException("TODO", e);
            }
            return true;
        }
        return false;
    }

    public Resource next() {
        Resource result;

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        result = next;
        next = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
