package net.oneandone.lavendel.processor;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultProcessorFactoryTest {

    protected ProcessorFactory processorFactory;

    @Before
    public void setUp() {
        processorFactory = new ProcessorFactory(null);
    }

    @Test
    public void testHtml() throws IOException {
        Processor htmlProcessor = processorFactory.createProcessor("text/html", null, "/");
        assertNotNull(htmlProcessor);
        assertTrue(htmlProcessor instanceof HtmlProcessor);
    }

    @Test
    public void testCss() throws IOException {
        Processor cssProcessor = processorFactory.createProcessor("text/css", null, "/");
        assertNotNull(cssProcessor);
        assertTrue(cssProcessor instanceof CssProcessor);
    }

    @Test
    public void testUnknownContentType() throws IOException {
        Processor nullProcessor = processorFactory.createProcessor("image/png", null, "/");
        assertNull(nullProcessor);
    }

    @Test
    public void testNullContentType() throws IOException {
        Processor nullProcessor = processorFactory.createProcessor(null, null, "/");
        assertNull(nullProcessor);
    }

    @Test
    public void testIllegalContentType() throws IOException {
        Processor nullProcessor = processorFactory.createProcessor("<%$=/>", null, "/");
        assertNull(nullProcessor);
    }

}
