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

import net.oneandone.lavender.config.Filter;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.xml.Xml;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PustefixModuleTest {
    private static World WORLD = new World();

    @Test
    public void oldModule() throws Exception {
        JarModuleConfig c;

        c = JarModuleConfig.load(new Xml(), null, getClass().getResourceAsStream("/old-module.xml"));
        assertEquals("ajax-addresscheck", c.getModuleName());
        assertEquals(Arrays.asList("abc/", "xyz/"), c.getStatics());
    }

    @Test
    public void newModule() throws Exception {
        JarModuleConfig c;

        c = JarModuleConfig.load(new Xml(), null, getClass().getResourceAsStream("/new-module.xml"));
        assertEquals("access-java", c.getModuleName());
    }

    @Test
    public void testExtract() throws Exception {
        URL url = getClass().getClassLoader().getResource("dummy.war");

        Map<String, Resource> resources = new HashMap<>();
        WarModule module = WarModule.create(new Filter(), WORLD.file(new File(url.toURI())).openZip());
        for (Resource resource : module) {
            resources.put(resource.getPath(), resource);
        }

        assertEquals(9, resources.size());

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

    @Ignore
    @Test
    public void testPerformance() throws JAXBException, IOException {

        URL url = getClass().getClassLoader().getResource("dummy.war");

        int num = 10000;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            ZipInputStream zipIn = new ZipInputStream(url.openStream());
            countEntries(zipIn);
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        System.out.println(num + " in " + t + "ms");

    }

    private void countEntries(ZipInputStream zipIn) throws IOException {
        int count = 0;
        while (zipIn.getNextEntry() != null) {
            count++;
        }
        assertEquals(32, count);
    }

}
