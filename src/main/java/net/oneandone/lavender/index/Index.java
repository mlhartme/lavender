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
package net.oneandone.lavender.index;

import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Properties;

/**
 * A set of labels.
 *
 * Format:
 *   originalPath = lavendelizedPath : md5
 *
 * where
 *   originalPath      source request path, that will be replaced by Lavender filter
 *   lavendelizedPath  path on Lavender Servers ... or some other data like a reference count
 *   md5               md5 sum of the resource
 */
public class Index implements Iterable<Label> {
    public static final String ALL_IDX = ".all.idx";

    public static final String ENCODING = "UTF-8";

    private static final String DELIMITER = ":";

    public static Index load(Node src) throws IOException {
        Index index;

        index = new Index();
        try (Reader reader = src.createReader()) {
            index.load(reader);
        }
        return index;
    }

    //--

    private final Properties properties;

    public Index() {
        properties = new Properties();
    }

    private void load(Reader in) throws IOException {
        properties.load(in);
    }

    //--

    public int size() {
        return properties.size();
    }

    /** @return true if the entry was new */
    public boolean add(Label label) {
        String original;
        String lavendelized;
        String md5hex;
        String prev;
        String next;

        original = label.getOriginalPath();
        if (original.isEmpty() || original.startsWith("/") || original.endsWith("/")) {
            throw new IllegalArgumentException("invalid originalPath: " + original);
        }
        lavendelized = label.getLavendelizedPath();
        if (lavendelized.isEmpty() || lavendelized.startsWith("/") || lavendelized.endsWith("/")) {
            throw new IllegalArgumentException("invalid lavendelizedPath" + lavendelized);
        }
        md5hex = Hex.encodeString(label.md5());
        if (lavendelized.contains(DELIMITER)) {
            throw new IllegalArgumentException(lavendelized);
        }
        next = lavendelized + DELIMITER + md5hex;
        prev = (String) properties.setProperty(original, next);
        if (prev != null) {
            if (!next.equals(prev)) {
                throw new IllegalArgumentException("conflicting values for originalPath: " + original + ": " + prev + " vs " + next);
            }
            // happens when modules are extracted and packaged into the war
            return false;
        } else {
            return true;
        }
    }

    //-- Index with reference counts

    public void addReference(String path, byte[] md5) {
        String value;
        int idx;
        int count;
        String md5hex;

        value = properties.getProperty(path);
        md5hex = Hex.encodeString(md5);
        if (value != null) {
            idx = value.indexOf(DELIMITER);
            count = Integer.parseInt(value.substring(0, idx)) + 1;
            if (!md5hex.equals(value.substring(idx + 1))) {
                throw new IllegalArgumentException("md5 mismatch for file " + path);
            }
        } else {
            count = 1;
        }
        properties.setProperty(path, Integer.toString(count) + DELIMITER + md5hex);
    }

    public boolean removeReferenceOpt(String path) {
        String value;
        int idx;
        int count;

        value = properties.getProperty(path);
        if (value == null) {
            return false;
        }
        idx = value.indexOf(DELIMITER);
        count = Integer.parseInt(value.substring(0, idx)) - 1;
        if (count == 0) {
            properties.remove(path);
        } else {
            properties.setProperty(path, Integer.toString(count) + DELIMITER + value.substring(idx + 1));
        }
        return true;
    }

    //--

    // from normal idx
    public boolean removeEntryOpt(String path) {
        return properties.remove(path) != null;
    }

    public Iterator<Label> iterator() {
        final Iterator<String> iter = properties.stringPropertyNames().iterator();
        return new Iterator<Label>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Label next() {
                return lookup(iter.next());
            }

            @Override
            public void remove() {
                throw new IllegalStateException();
            }
        };
    }

    public Label lookup(String originalPath) {
        String property;
        String lavendelizedPath;
        byte[] md5;
        int idx;

        property = properties.getProperty(originalPath);
        if (property == null) {
            return null;
        }
        idx = property.indexOf(':');
        if (idx == -1) {
            throw new IllegalStateException(originalPath + " = " + property);
        }
        lavendelizedPath = property.substring(0, idx);
        md5 = Hex.decode(property.substring(idx + 1).toCharArray());
        return new Label(originalPath, lavendelizedPath, md5);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Index) {
            return properties.equals(((Index) obj).properties);
        }
        return false;
    }

    public int hashCode() {
        return properties.hashCode();
    }

    public void save(Node indexFile) throws IOException {
        try (OutputStream dest = indexFile.createOutputStream()) {
            save(dest);
        }
    }

    /** dest will be not be closed */
    public void save(OutputStream dest) throws IOException {
        Writer writer = new OutputStreamWriter(dest, ENCODING);
        save(writer);
    }

    /** dest will not be closed */
    public void save(Writer writer) throws IOException {
        properties.store(writer, null);
    }

    public String toString() {
        StringWriter result;

        result = new StringWriter();
        try {
            save(result);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result.toString();
    }
}
