package com.oneandone.lavendel.filter;

import org.apache.tomcat.util.collections.EmptyEnumeration;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LavendelizeHttpServletRequestTest {

    private LavendelizeHttpServletRequest request;
    private HttpServletRequest wrappedRequest;

    @Before
    public void setUp() {
        wrappedRequest = mock(HttpServletRequest.class);
        request = new LavendelizeHttpServletRequest(wrappedRequest);
    }

    @Test
    public void testConstructor() {
        LavendelizeHttpServletRequest req = new LavendelizeHttpServletRequest(wrappedRequest);
        assertSame(wrappedRequest, req.getRequest());
    }

    @Test
    public void testGetHeader() throws IOException {
        when(wrappedRequest.getHeader("Accept-Encoding")).thenReturn("gzip");
        when(wrappedRequest.getHeader("Foo")).thenReturn("Bar");

        assertNull(request.getHeader("Accept-Encoding"));
        assertNotNull(request.getHeader("Foo"));
    }

    @Test
    public void testGetHeaders() throws IOException {
        when(wrappedRequest.getHeaders("Accept-Encoding")).thenReturn(new EmptyEnumeration());
        when(wrappedRequest.getHeaders("Foo")).thenReturn(Collections.enumeration(Arrays.asList("Bar")));

        assertNotNull(request.getHeaders("Accept-Encoding"));
        assertFalse(request.getHeaders("Accept-Encoding").hasMoreElements());
        assertNotNull(request.getHeaders("Foo"));
        assertTrue(request.getHeaders("Foo").hasMoreElements());
        assertEquals("Bar", request.getHeaders("Foo").nextElement());
    }

    @Test
    public void testGetHeaderNames() throws IOException {
        when(wrappedRequest.getHeaderNames()).thenReturn(
                Collections.enumeration(Arrays.asList("Accept-Encoding", "Foo")));

        Enumeration<String> headerNames = request.getHeaderNames();
        assertNotNull(headerNames);
        assertTrue(headerNames.hasMoreElements());
        assertEquals("Foo", headerNames.nextElement());
        assertFalse(headerNames.hasMoreElements());
    }

}
