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
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.xml.Xml;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeModuleTest {
    private static World WORLD;

    static {
        try {
            WORLD = World.create(false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void directory() throws Exception {
        FileNode dir;
        Module module;
        Iterator<Resource> iter;
        Resource resource;

        dir = WORLD.guessProjectHome(getClass()).join("src/test/module");
        module = new NodeModule(Module.TYPE, "foo", false, "", "", WORLD.filter().includeAll()) {
            @Override
            protected Map<String, Node> loadEntries() throws Exception {
                Map<String, Node> result;

                result = new HashMap<>();
                for (Node node : dir.find(getFilter().predicate(Predicate.FILE))) {
                    result.put(node.getRelative(dir), node);
                }
                return result;
            }
        };
        iter = module.iterator();
        resource = iter.next();
        assertEquals("sub/main.css", resource.getPath());
        resource = iter.next();
        assertEquals("vi_login_now.jpg", resource.getPath());
        assertFalse(iter.hasNext());
    }

    @Test
    public void oldConfig() throws Exception {
        JarConfig c;

        c = JarConfig.load(new Xml(), null, getClass().getResourceAsStream("/old-module.xml"));
        assertEquals("ajax-addresscheck", c.getModuleName());
        assertEquals(Arrays.asList("abc/", "xyz/"), c.getStatics());
    }

    @Test
    public void newConfig() throws Exception {
        JarConfig c;

        c = JarConfig.load(new Xml(), null, getClass().getResourceAsStream("/new-module.xml"));
        assertEquals("access-java", c.getModuleName());
    }

    @Test
    public void war() throws Exception {
        Map<String, Resource> resources;
        URL url;
        NodeModule module;

        resources = new HashMap<>();
        url = getClass().getClassLoader().getResource("dummy.war");
        module = NodeModule.warModule(new WarConfig(Arrays.asList("img", "modules")), new Filter().includeAll(),
                WORLD.file(new File(url.toURI())).openZip());
        for (Resource resource : module) {
            resources.put(resource.getPath(), resource);
        }

        assertTrue(resources.containsKey("img/sub/check_grey.gif"));
        assertTrue(resources.containsKey("img/sub/check_green.gif"));
        assertTrue(resources.containsKey("img/btn_weiter.gif"));
        assertTrue(resources.containsKey("modules/stageassistent/img/open.gif"));
        assertTrue(resources.containsKey("modules/stageassistent/img/minimize.gif"));
        assertTrue(resources.containsKey("modules/stageassistent/img/close.gif"));
        assertTrue(resources.containsKey("modules/frontend-tools/img/delete.png"));
        assertTrue(resources.containsKey("modules/frontend-tools/img/accept.png"));
        assertTrue(resources.containsKey("modules/frontend-tools/img/cross.png"));
    }
}
