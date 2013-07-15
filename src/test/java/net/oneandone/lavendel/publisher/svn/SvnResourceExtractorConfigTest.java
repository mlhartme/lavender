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
package net.oneandone.lavendel.publisher.svn;

import org.junit.Test;

import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SvnResourceExtractorConfigTest {
    @Test
    public void empty() {
        assertEquals(0, SvnExtractorConfig.parse(new Properties()).size());
    }

    @Test
    public void one() {
        Properties props;
        Collection<SvnExtractorConfig> result;
        SvnExtractorConfig config;

        props = new Properties();
        props.put("svn.foo", "svn");
        props.put("svn.foo.pathPrefix", "prefix");
        props.put("svn.foo.lavendelize", "false");
        result = SvnExtractorConfig.parse(props);
        assertEquals(1, result.size());
        config = result.iterator().next();
        assertEquals("foo", config.name);
        assertFalse(config.lavendelize);
        assertEquals("prefix", config.pathPrefix);
        assertEquals("svn", config.svn);
    }

    @Test
    public void more() {
        Properties props;
        Collection<SvnExtractorConfig> result;

        props = new Properties();
        props.put("svn.foo", "1");
        props.put("svn.bar", "2");
        props.put("svn.baz", "3");
        result = SvnExtractorConfig.parse(props);
        assertEquals(3, result.size());
    }

    @Test
    public void simplify() {
        check("", "");
        check("ab/cd/ef", "ab/cd/ef");
        check("a/b", "a/trunk/b");
        check("a/b", "a/tags/foo-1.2/b");
    }

    private void check(String expected, String orig) {
        assertEquals(expected, SvnExtractorConfig.simplify(orig));
    }
}
