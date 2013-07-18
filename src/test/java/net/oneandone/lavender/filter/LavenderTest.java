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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LavenderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void init() throws Exception {
        temporaryFolder.newFolder("lavender");
        temporaryFolder.newFolder("lavender/WEB-INF");
        File indexFile = temporaryFolder.newFile("lavender" + Lavender.LAVENDEL_IDX);
        File nodesFile = temporaryFolder.newFile("lavender" + Lavender.LAVENDEL_NODES);
        String data = "";
        data += "http://s1.uicdn.net/m1" + IOUtils.LINE_SEPARATOR;
        data += "https://s1.uicdn.net/m1" + IOUtils.LINE_SEPARATOR;
        data += "http://s2.uicdn.net/m1/" + IOUtils.LINE_SEPARATOR;
        data += "https://s2.uicdn.net/m1/" + IOUtils.LINE_SEPARATOR;
        data += IOUtils.LINE_SEPARATOR;
        FileUtils.writeStringToFile(nodesFile, data);

        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getResource(Lavender.LAVENDEL_IDX)).thenReturn(indexFile.toURI().toURL());
        when(servletContext.getResource(Lavender.LAVENDEL_NODES)).thenReturn(nodesFile.toURI().toURL());
        when(filterConfig.getServletContext()).thenReturn(servletContext);

        Lavender filter = new Lavender();
        filter.init(filterConfig);
        filter.lazyInit();
        assertNotNull(filter.processorFactory);
    }
}
