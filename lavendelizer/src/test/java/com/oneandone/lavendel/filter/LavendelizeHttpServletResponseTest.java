package com.oneandone.lavendel.filter;

import com.oneandone.lavendel.processor.ProcessorFactory;
import com.oneandone.lavendel.processor.Processor;
import org.apache.commons.io.output.NullWriter;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LavendelizeHttpServletResponseTest {

    private LavendelizeHttpServletResponse response;
    private HttpServletResponse wrappedResponse;
    private ProcessorFactory processorFactory;

    @Before
    public void setUp() {
        wrappedResponse = mock(HttpServletResponse.class);
        processorFactory = mock(ProcessorFactory.class);
        URI requestURI = URI.create("http://localhost:8080/a/b/c.html");
        String contextPath = "/a/";
        response = new LavendelizeHttpServletResponse(wrappedResponse, processorFactory, requestURI, null, contextPath, false);
    }

    @Test
    public void testConstructor() {
        URI requestURI = URI.create("http://localhost:8080/a/b/c.html");
        String contextPath = "/a/";
        LavendelizeHttpServletResponse res = new LavendelizeHttpServletResponse(wrappedResponse, processorFactory,
                requestURI, null, contextPath, false);

        assertSame(wrappedResponse, res.getResponse());
        assertSame(processorFactory, res.processorFactory);
        assertSame(requestURI, res.requestURI);
        assertNull(res.outputStream);
        assertNull(res.writer);
    }

    @Test
    public void testGetWriter() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(new NullWriter());
        when(wrappedResponse.getWriter()).thenReturn(wrappedWriter);

        PrintWriter writer = response.getWriter();

        // subsequent calls must return the same writer object
        assertSame(writer, response.getWriter());
        assertSame(writer, response.getWriter());
    }

    @Test
    public void testGetLavendelizedWriter() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(new NullWriter());
        when(wrappedResponse.getWriter()).thenReturn(wrappedWriter);
        Processor processor = mock(Processor.class);
        when(processorFactory.createProcessor(anyString(), any(URI.class), anyString())).thenReturn(processor);

        // the returned writer is a PrintWriter that wraps the LavendelizeWriter
        PrintWriter writer = response.getWriter();
        assertNotNull(writer);
        assertNotSame(wrappedWriter, writer);

        // subsequent calls must return the same writer object
        assertSame(writer, response.getWriter());
        assertSame(writer, response.getWriter());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOutputStreamAfterGetWriter() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(new NullWriter());
        when(wrappedResponse.getWriter()).thenReturn(wrappedWriter);

        response.getWriter();
        response.getOutputStream();
    }

    @Test
    public void testGetOutputStream() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(new NullWriter());
        when(wrappedResponse.getWriter()).thenReturn(wrappedWriter);
        when(wrappedResponse.getCharacterEncoding()).thenReturn("UTF-8");
        ServletOutputStream outputStream = response.getOutputStream();

        // subsequent calls must return the same output stream object
        assertSame(outputStream, response.getOutputStream());
        assertSame(outputStream, response.getOutputStream());
    }

    @Test
    public void testGetLavendelizedOutputStream() throws IOException {
        ServletOutputStream wrappedOutputStream = new ServletOutputStream() {
            public void write(int b) throws IOException {
            }
        };
        when(wrappedResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(wrappedResponse.getCharacterEncoding()).thenReturn("UTF-8");
        when(wrappedResponse.getOutputStream()).thenReturn(wrappedOutputStream);
        Processor processor = mock(Processor.class);
        when(processorFactory.createProcessor(anyString(), any(URI.class), anyString())).thenReturn(processor);

        ServletOutputStream outputStream = response.getOutputStream();
        assertNotNull(outputStream);
        assertTrue(outputStream instanceof DeferredOutputStream);

        // subsequent calls must return the same output stream object
        assertSame(outputStream, response.getOutputStream());
        assertSame(outputStream, response.getOutputStream());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetWriterAfterGetOutputStream() throws IOException {
        ServletOutputStream wrappedOutputStream = new ServletOutputStream() {
            public void write(int b) throws IOException {
            }
        };
        when(wrappedResponse.getCharacterEncoding()).thenReturn("UTF-8");
        when(wrappedResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(wrappedResponse.getOutputStream()).thenReturn(wrappedOutputStream);

        response.getOutputStream();
        response.getWriter();
    }

    @Test
    public void testHeader() throws IOException {
        response.setHeader("a", "foo");
        response.setIntHeader("b", 2);
        response.setDateHeader("c", 1234567890);

        response.addHeader("a", "foo");
        response.addIntHeader("b", 2);
        response.addDateHeader("c", 1234567890);

        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals(3, headers.size());
        assertTrue(headers.containsKey("a"));
        assertEquals("foo", headers.get("a"));
        assertTrue(headers.containsKey("b"));
        assertEquals("2", headers.get("b"));
        assertTrue(headers.containsKey("c"));
        assertEquals("1234567890", headers.get("c"));
    }

}
