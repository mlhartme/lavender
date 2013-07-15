package com.oneandone.lavendel.publisher.pustefix;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PustefixProjectConfigTest {

    private static PustefixProjectConfig config;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        URL url = PustefixProjectConfigTest.class.getClassLoader().getResource("dummy.war");
        config = new PustefixProjectConfig(new File(url.toURI()));
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
