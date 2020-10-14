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

import net.oneandone.lavender.filter.processor.LavenderProcessorFactory;
import net.oneandone.lavender.filter.processor.Processor;
import net.oneandone.sushi.io.MultiWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LavendelizeHttpServletResponseTest {

    private LavendelizeHttpServletResponse response;
    private HttpServletResponse wrappedResponse;
    private LavenderProcessorFactory processorFactory;

    @BeforeEach
    public void setUp() {
        wrappedResponse = mock(HttpServletResponse.class);
        processorFactory = mock(LavenderProcessorFactory.class);
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
        PrintWriter wrappedWriter = new PrintWriter(MultiWriter.createNullWriter());
        when(wrappedResponse.getWriter()).thenReturn(wrappedWriter);

        PrintWriter writer = response.getWriter();

        // subsequent calls must return the same writer object
        assertSame(writer, response.getWriter());
        assertSame(writer, response.getWriter());
    }

    @Test
    public void testGetLavendelizedWriter() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(MultiWriter.createNullWriter());
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

    @Test()
    public void testGetOutputStreamAfterGetWriter() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(MultiWriter.createNullWriter());
        when(wrappedResponse.getWriter()).thenReturn(wrappedWriter);

        assertThrows(IllegalStateException.class, () -> {
            response.getWriter();
            response.getOutputStream();
        });
    }

    @Test
    public void testGetOutputStream() throws IOException {
        PrintWriter wrappedWriter = new PrintWriter(MultiWriter.createNullWriter());
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
            public void write(int b) {
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

    @Test()
    public void testGetWriterAfterGetOutputStream() throws IOException {
        ServletOutputStream wrappedOutputStream = new ServletOutputStream() {
            public void write(int b) {
            }
        };
        when(wrappedResponse.getCharacterEncoding()).thenReturn("UTF-8");
        when(wrappedResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(wrappedResponse.getOutputStream()).thenReturn(wrappedOutputStream);

        assertThrows(IllegalStateException.class, () -> {
            response.getOutputStream();
            response.getWriter();
        });
    }

    @Test
    public void testHeader() {
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
