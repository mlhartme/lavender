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
package net.oneandone.lavender.publisher.config;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FilterTest {
    @Test
    public void testNonDefaultConstructor() {
        Collection<String> includes = Arrays.asList("*.jpg", "*.gif");
        Collection<String> excludes = Arrays.asList("**/close.gif");
        Filter config = new Filter(includes, excludes);

        assertNotNull(config.getIncludes());
        assertEquals(2, config.getIncludes().size());
        assertTrue(config.getIncludes().contains("*.jpg"));
        assertTrue(config.getIncludes().contains("*.gif"));
        assertNotNull(config.getExcludes());
        assertEquals(1, config.getExcludes().size());
        assertTrue(config.getExcludes().contains("**/close.gif"));
    }

    @Test
    public void testSetterGetter() {
        Filter config = new Filter();
        assertNull(config.getIncludes());
        assertNull(config.getExcludes());

        config.setIncludes("*.jpg", "*.gif");
        config.setExcludes("**/close.gif");
        assertNotNull(config.getIncludes());
        assertNotNull(config.getExcludes());

        Collection<String> includes = null;
        Collection<String> excludes = null;
        config.setIncludes(includes);
        config.setExcludes(excludes);
        assertNull(config.getIncludes());
        assertNull(config.getExcludes());
    }

    @Test
    public void testToString() {
        Filter config = new Filter();
        assertNotNull(config.toString());
    }

    @Test
    public void testIsIncluded() {
        Collection<String> includes = Arrays.asList("*.jpg", "*.gif");
        Collection<String> excludes = Arrays.asList("**/close.gif", "http://lavendel.schlund.de/*");
        Filter config = new Filter(includes, excludes);

        assertTrue(config.isIncluded("test.jpg"));
        assertTrue(config.isIncluded("/test.jpg"));
        assertTrue(config.isIncluded("/a/test.jpg"));
        assertTrue(config.isIncluded("test.gif"));
        assertTrue(config.isIncluded("/a/b/c/test.gif"));
        assertTrue(config.isIncluded("http://img.schlund.de/test.gif"));

        assertFalse(config.isIncluded("test.css"));
        assertFalse(config.isIncluded("/close.gif"));
        assertFalse(config.isIncluded("/a/close.gif"));
        assertFalse(config.isIncluded("/a/b/c/close.gif"));
        assertFalse(config.isIncluded("http://lavendel.schlund.de/test.gif"));
    }

    @Test
    public void properties() {
        Properties properties;
        Filter config;
        List<String> dflt;

        properties = new Properties();
        properties.put("foo.includeExtensions", "a, b");
        properties.put("foo.excludeExtensions", "c");
        dflt = new ArrayList<String>();
        config = Filter.forProperties(properties, "empty", dflt);
        assertEquals(dflt, config.getIncludes());
        assertNull(config.getExcludes());
        config = Filter.forProperties(properties, "foo", dflt);
        assertEquals(Arrays.asList("*.a", "*.b"), config.getIncludes());
        assertEquals(Arrays.asList("*.c"), config.getExcludes());
    }
}
