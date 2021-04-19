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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModulePropertiesTest {
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

    private static Properties pomInfo() throws IOException {
        Properties p;

        p = new Properties();
        p.put("ethernet", Separator.COMMA.join(ModuleProperties.ethernet()));
        p.put("basedir", "someDirectory");
        return p;
    }

}
