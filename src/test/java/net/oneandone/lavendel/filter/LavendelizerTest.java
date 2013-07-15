package net.oneandone.lavendel.filter;

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

public class LavendelizerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void init() throws Exception {
        temporaryFolder.newFolder("lavendel");
        temporaryFolder.newFolder("lavendel/WEB-INF");
        File indexFile = temporaryFolder.newFile("lavendel" + Lavendelizer.LAVENDEL_IDX);
        File nodesFile = temporaryFolder.newFile("lavendel" + Lavendelizer.LAVENDEL_NODES);
        String data = "";
        data += "http://s1.uicdn.net/m1" + IOUtils.LINE_SEPARATOR;
        data += "https://s1.uicdn.net/m1" + IOUtils.LINE_SEPARATOR;
        data += "http://s2.uicdn.net/m1/" + IOUtils.LINE_SEPARATOR;
        data += "https://s2.uicdn.net/m1/" + IOUtils.LINE_SEPARATOR;
        data += IOUtils.LINE_SEPARATOR;
        FileUtils.writeStringToFile(nodesFile, data);

        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getResource(Lavendelizer.LAVENDEL_IDX)).thenReturn(indexFile.toURI().toURL());
        when(servletContext.getResource(Lavendelizer.LAVENDEL_NODES)).thenReturn(nodesFile.toURI().toURL());
        when(filterConfig.getServletContext()).thenReturn(servletContext);

        Lavendelizer filter = new Lavendelizer();
        filter.init(filterConfig);

        assertNotNull(filter.processorFactory);
    }
}
