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
package net.oneandone.lavender.filter;

import net.oneandone.lavender.config.Properties;
import net.oneandone.lavender.modules.Module;
import net.oneandone.sushi.fs.*;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class LavenderTest {

    private static final World WORLD = new World(false);

    private FileNode root;

    private FileNode lavender;

    private Lavender filter;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private ServletContext servletContext;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        root = WORLD.getTemp().createTempDirectory();

        lavender = root.join("lavender");
        lavender.mkdir();
        lavender.join("WEB-INF").mkdir();

        filter = new Lavender();
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn(lavender.getAbsolute());

        System.setProperty("lavender.properties", getClass().getClassLoader().getResource("lavender.properties").getFile());
    }

    @After
    public void tearDown() throws NodeNotFoundException, DeleteException {
        if (root != null) {
            root.deleteTree();
        }
    }

    @Test
    public void initShouldUseProductionFilter() throws Exception {
        givenFile(Lavender.LAVENDER_IDX);
        givenFile(Lavender.LAVENDER_NODES, "http://s1.uicdn.net/m1", "https://s1.uicdn.net/m1");


        filter.init(filterConfig);


        assertNotNull(filter.processorFactory);
        assertNull(filter.develModules);
    }

    @Test
    public void initShouldUseDevelopmentFilter() throws Exception {
        Lavender filterSpy = Mockito.spy(filter);
        doReturn(new ArrayList<Module>()).when(filterSpy).loadModulesFromWebapp(any(Node.class), any(Properties.class), any(FileNode.class));


        filterSpy.init(filterConfig);


        assertNull(filterSpy.processorFactory);
        assertNotNull(filterSpy.develModules);
    }

    private void givenFile(String filename, String... lines) throws IOException {
        FileNode file = lavender.join(filename);
        file.mkfile();
        file.writeLines(lines);

        when(servletContext.getResource("/" + filename)).thenReturn(file.getURI().toURL());
    }

}
