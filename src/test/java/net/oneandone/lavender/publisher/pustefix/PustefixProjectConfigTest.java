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
package net.oneandone.lavender.publisher.pustefix;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PustefixProjectConfigTest {
    private static final World WORLD = new World();
    private static PustefixProjectConfig config;

    @BeforeClass
    public static void setup() throws URISyntaxException, IOException, JAXBException {
        FileNode war = WORLD.guessProjectHome(PustefixProjectConfigTest.class).join("src/test/resources/dummy.war");
        config = new PustefixProjectConfig(war.openZip());
    }

    @Test
    public void testProjectName() {
        assertEquals("nightclub-eue-de", config.getProjectName());
    }

    @Test
    public void testProjectPublicResource() {
        assertTrue(config.isPublicResource("img/test.gif"));
        assertFalse(config.isPublicResource("protected/secret.pdf"));
        assertFalse(config.isPublicResource("WEB-INF/lib/frontend-tools-3.1.1.jar"));
    }

    @Test
    public void testModuleConfigs() {
        assertNotNull(config.getModules());
        assertEquals(2, config.getModules().size());
        assertTrue(config.getModules().containsKey("WEB-INF/lib/frontend-elements-0.4.25.jar"));
        assertTrue(config.getModules().containsKey("WEB-INF/lib/frontend-tools-3.1.1.jar"));
    }

    @Test
    public void testIsModule() {
        assertTrue(config.isModule("WEB-INF/lib/frontend-elements-0.4.25.jar"));
        assertTrue(config.isModule("WEB-INF/lib/frontend-tools-3.1.1.jar"));
        assertFalse(config.isModule("WEB-INF/lib/activation-1.1.jar"));
        assertFalse(config.isModule("WEB-INF/lib/no-such.jar"));
        assertFalse(config.isModule("nonsense"));
        assertFalse(config.isModule(null));
    }

    @Test
    public void testModuleName() {
        PustefixModuleConfig moduleConfig1 = config.getModuleConfig("WEB-INF/lib/frontend-elements-0.4.25.jar");
        assertNotNull(moduleConfig1);
        assertEquals("frontend-elements", moduleConfig1.getModuleName());

        PustefixModuleConfig moduleConfig2 = config.getModuleConfig("WEB-INF/lib/frontend-tools-3.1.1.jar");
        assertNotNull(moduleConfig2);
        assertEquals("frontend-tools", moduleConfig2.getModuleName());
    }


    @Test
    public void testModulePublicResource() {
        PustefixModuleConfig moduleConfig = config.getModuleConfig("WEB-INF/lib/frontend-elements-0.4.25.jar");
        assertNotNull(moduleConfig);
        assertTrue(moduleConfig.isPublicResource("PUSTEFIX-INF/img/error/error.gif"));
        assertTrue(moduleConfig.isPublicResource("style/error/error.css"));
        assertFalse(moduleConfig.isPublicResource("style/ie6/button.css"));
        assertFalse(moduleConfig.isPublicResource("htdocs/error400-de.html"));
    }

    @Test
    public void testGetPath() {
        PustefixModuleConfig moduleConfig = config.getModuleConfig("WEB-INF/lib/frontend-elements-0.4.25.jar");
        assertEquals("modules/frontend-elements/img/error/error.gif", moduleConfig.getPath("PUSTEFIX-INF/img/error/error.gif"));
    }

}
