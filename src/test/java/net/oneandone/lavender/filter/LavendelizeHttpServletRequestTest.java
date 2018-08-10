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

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

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
    public void testGetHeader() {
        when(wrappedRequest.getHeader("Accept-Encoding")).thenReturn("gzip");
        when(wrappedRequest.getHeader("Foo")).thenReturn("Bar");

        assertNull(request.getHeader("Accept-Encoding"));
        assertNotNull(request.getHeader("Foo"));
    }

    @Test
    public void testGetHeaders() {
        when(wrappedRequest.getHeaders("Accept-Encoding")).thenReturn(new Vector().elements());
        when(wrappedRequest.getHeaders("Foo")).thenReturn(Collections.enumeration(Arrays.asList("Bar")));

        assertNotNull(request.getHeaders("Accept-Encoding"));
        assertFalse(request.getHeaders("Accept-Encoding").hasMoreElements());
        assertNotNull(request.getHeaders("Foo"));
        assertTrue(request.getHeaders("Foo").hasMoreElements());
        assertEquals("Bar", request.getHeaders("Foo").nextElement());
    }

    @Test
    public void testGetHeaderNames() {
        when(wrappedRequest.getHeaderNames()).thenReturn(
                Collections.enumeration(Arrays.asList("Accept-Encoding", "Foo")));

        Enumeration<String> headerNames = request.getHeaderNames();
        assertNotNull(headerNames);
        assertTrue(headerNames.hasMoreElements());
        assertEquals("Foo", headerNames.nextElement());
        assertFalse(headerNames.hasMoreElements());
    }

}
