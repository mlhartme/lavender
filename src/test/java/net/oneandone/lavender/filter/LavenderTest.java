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
package net.oneandone.lavender.filter;

import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LavenderTest {
    private static final World WORLD = World.createMinimal();

    private FileNode root;

    private FileNode lavenderRoot;

    private Lavender lavenderFilter;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private ServletContext servletContext;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        root = WORLD.getTemp().createTempDirectory();

        lavenderRoot = root.join("lavenderRoot");
        lavenderRoot.mkdir();
        lavenderRoot.join("WEB-INF").mkdir();

        lavenderFilter = new Lavender();

        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn(lavenderRoot.getAbsolute());
    }

    @AfterEach
    public void tearDown() throws NodeNotFoundException, DeleteException {
        if (root != null) {
            root.deleteTree();
        }
    }

    @Test
    public void initShouldBeInProduction() throws Exception {
        givenFile(Lavender.LAVENDER_IDX);
        givenFile(Lavender.LAVENDER_NODES, "http://s1.uicdn.net/m1", "https://s1.uicdn.net/m1");


        lavenderFilter.init(filterConfig);


        assertTrue(lavenderFilter.getProd());
        assertEquals(-1, lavenderFilter.getModules());
    }

    @Test
    public void initShouldUseDevelopmentFilter() throws Exception {
        DevelopmentFilter developmentFilterMock = mock(DevelopmentFilter.class);
        Mockito.when(developmentFilterMock.getModulesCount()).thenReturn(5);

        Lavender lavenderFilterSpy = Mockito.spy(lavenderFilter);
        doReturn(developmentFilterMock).when(lavenderFilterSpy).createDevelopmentFilter();


        lavenderFilterSpy.init(filterConfig);


        assertFalse(lavenderFilterSpy.getProd());
        assertEquals(5, lavenderFilterSpy.getModules());
    }

    @Test
    public void initShouldUseProductionAndDevelopmentFilter() throws Exception {
        givenFile(Lavender.LAVENDER_IDX);
        givenFile(Lavender.LAVENDER_NODES, "http://s1.uicdn.net/m1", "https://s1.uicdn.net/m1");

        System.setProperty("lavender.allowProdDevMixMode", "true");

        ProductionFilter productionFilterMock = mock(ProductionFilter.class);

        DevelopmentFilter developmentFilterMock = mock(DevelopmentFilter.class);
        Mockito.when(developmentFilterMock.getModulesCount()).thenReturn(5);

        Lavender lavenderFilterSpy = Mockito.spy(lavenderFilter);
        doReturn(productionFilterMock).when(lavenderFilterSpy).createProductionFilter();
        doReturn(developmentFilterMock).when(lavenderFilterSpy).createDevelopmentFilter();


        lavenderFilterSpy.init(filterConfig);


        assertFalse(lavenderFilterSpy.getProd());
        assertEquals(5, lavenderFilterSpy.getModules());
        verify(productionFilterMock, times(1)).init(any());
        verify(developmentFilterMock, times(1)).init(any());
    }

    private void givenFile(String filename, String... lines) throws IOException {
        FileNode file = lavenderRoot.join(filename);
        file.mkfile();
        file.writeLines(lines);

        when(servletContext.getResource("/" + filename)).thenReturn(file.getUri().toURL());
    }

}
