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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LavenderTest {
    private static final World WORLD = new World();

    @Test
    public void init() throws Exception {
        FileNode root;
        FileNode lavender;

        root = WORLD.getTemp().createTempDirectory();
        lavender = root.join("lavender");
        lavender.mkdir();
        lavender.join("WEB-INF").mkdir();

        FileNode indexFile = root.join("lavender", Lavender.LAVENDEL_IDX).mkfile();
        FileNode nodesFile = root.join("lavender", Lavender.LAVENDEL_NODES);

        nodesFile.writeLines("http://s1.uicdn.net/m1",
                "https://s1.uicdn.net/m1",
                "http://s2.uicdn.net/m1",
                "https://s2.uicdn.net/m1");

        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getResource("/" + Lavender.LAVENDEL_IDX)).thenReturn(indexFile.getURI().toURL());
        when(servletContext.getResource("/" + Lavender.LAVENDEL_NODES)).thenReturn(nodesFile.getURI().toURL());
        when(servletContext.getRealPath("")).thenReturn(lavender.getAbsolute());
        when(filterConfig.getServletContext()).thenReturn(servletContext);

        Lavender filter = new Lavender();
        filter.init(filterConfig);
        filter.lazyInit();
        assertNotNull(filter.processorFactory);
    }
}
