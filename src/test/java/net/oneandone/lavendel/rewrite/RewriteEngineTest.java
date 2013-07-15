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
package net.oneandone.lavendel.rewrite;

import net.oneandone.lavendel.index.Index;
import net.oneandone.lavendel.index.Label;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RewriteEngineTest {
    private Index index;
    private UrlCalculator urlCalculator;
    private RewriteEngine engine;

    @Before
    public void setUp() throws IOException {
        index = mock(Index.class);
        urlCalculator = mock(UrlCalculator.class);
        engine = new RewriteEngine(index, urlCalculator);
    }

    @Test(expected = NullPointerException.class)
    public void testDefaultRewriteEngineNullParameters() throws IOException, URISyntaxException {
        engine = new RewriteEngine(null, null);
        engine.rewrite(new URI("abc"), new URI("http://a.b.c"), "");
    }

    @Test
    public void testRewriteWithoutCache() throws IOException, URISyntaxException {

        String path1 = "abc";
        String path2 = "def";
        URI baseURI = URI.create("");
        String contextPath = "/";

        Label label1 = mock(Label.class);
        Label label2 = mock(Label.class);
        when(index.lookup(baseURI.resolve(URI.create(path1)).toASCIIString())).thenReturn(label1);
        when(index.lookup(baseURI.resolve(URI.create(path2)).getPath())).thenReturn(label2);
        when(urlCalculator.calculateURL(label1, baseURI)).thenReturn(new URI("http://a.b/c"));
        when(urlCalculator.calculateURL(label2, baseURI)).thenReturn(new URI("http://d.e/f"));

        URI uri1 = engine.rewrite(URI.create(path1), baseURI, contextPath);
        assertNotNull(uri1);
        assertEquals("http://a.b/c", uri1.toString());

        URI uri2 = engine.rewrite(URI.create(path2), baseURI, contextPath);
        assertNotNull(uri2);
        assertEquals("http://d.e/f", uri2.toString());

        verify(index, times(1)).lookup(baseURI.resolve(URI.create(path1)).toASCIIString());
        verify(index, times(1)).lookup(baseURI.resolve(URI.create(path2)).toASCIIString());
        verify(urlCalculator, times(1)).calculateURL(label1, baseURI);
        verify(urlCalculator, times(1)).calculateURL(label2, baseURI);
    }

    @Test
    public void testNoRewriteOfAbsoulteURI() throws IOException, URISyntaxException {

        URI reference = URI.create("http://x.y.z:1234/index.html");
        URI baseURI = URI.create("");
        String contextPath = "/";

        URI uri = engine.rewrite(reference, baseURI, contextPath);
        assertSame(reference, uri);
    }

    @Test
    public void testResolve() throws Exception {
        URI reference = URI.create("img/close.gif");
        URI baseURI = URI.create("http://localhost:80");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/"));
    }

    @Test
    public void testResolveWithNullPath() throws Exception {
        URI reference = URI.create("mailto:michael.hartmeier@1und1.de");
        URI baseURI = URI.create("http://localhost:80");
        assertNull(engine.resolve(reference, baseURI, "/"));
    }

    @Test
    public void testResolveRelativeReferenceRootcontext() throws Exception {
        URI reference = URI.create("img/close.gif");
        URI baseURI = URI.create("http://localhost:80/");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/"));
    }

    @Test
    public void testResolveAbsoluteReferenceRootContext() throws Exception {
        URI reference = URI.create("/img/close.gif");
        URI baseURI = URI.create("http://localhost:80/");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/"));
    }

    @Test
    public void testResolveRelativeReferenceSubContext() throws Exception {
        URI reference = URI.create("img/close.gif");
        URI baseURI = URI.create("http://localhost:80/app/");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/app/"));
    }

    @Test
    public void testResolveAbsoluteReferenceSubContext() throws Exception {
        URI reference = URI.create("/app/img/close.gif");
        URI baseURI = URI.create("http://localhost:80/app/");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/app/"));
    }

    @Test
    public void testResolveChildRelativeReferenceSubContext() throws Exception {
        URI reference = URI.create("close.gif");
        URI baseURI = URI.create("http://localhost:80/app/img/");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/app/"));
    }

    @Test
    public void testResolveParentRelativeReferenceSubContext() throws Exception {
        URI reference = URI.create("../img/close.gif");
        URI baseURI = URI.create("http://localhost:80/app/img/");
        assertEquals("img/close.gif", engine.resolve(reference, baseURI, "/app/"));
    }

    @Test
    public void testInvalid() throws Exception {
        assertEquals("'", engine.rewrite("'", URI.create("http://localhost:80/app/img/"), "/app/"));
    }
}
