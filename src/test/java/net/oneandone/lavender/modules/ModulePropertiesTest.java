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

import net.oneandone.sushi.util.Separator;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ModulePropertiesTest {
    @Test
    public void legacyEmpty() throws IOException {
        Properties props;

        props= new Properties();
        props.put("pustefix.relative", "base");
        props.put("pustefix.excludes", "**/*");
        assertEquals(0, ModuleProperties.parse(true, props, pomInfo()).configs.size());
    }

    @Test
    public void one() throws IOException {
        Properties props;
        ModuleProperties result;
        ScmProperties config;

        props = new Properties();
        props.put("scm.foo", "svn");
        props.put("scm.foo.targetPathPrefix", "prefix");
        props.put("scm.foo.lavendelize", "false");
        result = ModuleProperties.parse(false, props, pomInfo());
        assertEquals(1, result.configs.size());
        config = result.configs.iterator().next();
        assertEquals("foo", config.name);
        assertFalse(config.lavendelize);
        assertEquals("prefix", config.targetPathPrefix);
        assertEquals("svn", config.connectionProd);
        assertEquals("svn", config.connectionDevel);
    }

    //--

    @Test
    public void legacyOne() throws IOException {
        Properties props;
        ModuleProperties result;
        ScmProperties config;

        props = new Properties();
        props.put("pustefix.relative", "relative");
        props.put("pustefix.excludes", "**/*");
        props.put("svn.foo", "svn");
        props.put("svn.foo.targetPathPrefix", "prefix");
        props.put("svn.foo.lavendelize", "false");
        props.put("svn.foo.relative", "sub");
        result = ModuleProperties.parse(false, props, pomInfo());
        assertEquals(1, result.configs.size());
        config = result.configs.iterator().next();
        assertEquals("foo", config.name);
        assertFalse(config.lavendelize);
        assertEquals("prefix", config.targetPathPrefix);
        assertEquals("svn", config.connectionProd);
        assertEquals("svn", config.connectionDevel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void legacyUndefinedNormal() throws IOException {
        Properties props;

        props = new Properties();
        props.put("pustefix.excludes", "**/*");
        ModuleProperties.parse(true, props, pomInfo());
    }

    @Test(expected = IllegalArgumentException.class)
    public void legacyUndefinedSvn() throws IOException {
        Properties props;

        props = new Properties();
        props.put("pustefix.relative", "foo");
        props.put("pustefix.excludes", "**/*");
        props.put("svn.module.nosuchkey", "bla");
        ModuleProperties.parse(true, props, pomInfo());
    }

    @Test
    public void legacyMore() throws IOException {
        Properties props;
        Collection<ScmProperties> result;

        props = new Properties();
        props.put("pustefix.relative", "foo");
        props.put("pustefix.excludes", "**/*");
        props.put("svn.foo", "1");
        props.put("svn.bar", "2");
        props.put("svn.baz", "3");
        result = ModuleProperties.parse(true, props, pomInfo()).configs;
        assertEquals(3, result.size());
    }

    private static Properties pomInfo() throws IOException {
        Properties p;

        p = new Properties();
        p.put("ethernet", Separator.COMMA.join(ModuleProperties.ethernet()));
        p.put("basedir", "someDirectory");
        return p;
    }

}
