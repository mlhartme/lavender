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
package net.oneandone.lavender.filter.processor;

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.index.Resource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RewriteEngineTest {
    private Index index;
    private RewriteEngine engine;

    @Before
    public void setUp() throws IOException {
        index = mock(Index.class);
        engine = new RewriteEngine(index);
    }

    @Test(expected = NullPointerException.class)
    public void testDefaultRewriteEngineNullParameters() throws IOException, URISyntaxException {
        engine = new RewriteEngine(null);
        engine.rewrite(new URI("abc"), new URI("http://a.b.c"), "");
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

    //--


    @Test
    public void calculateURL() throws IOException {
        RewriteEngine engine;
        byte[] md5;
        String md5str;
        Label label;
        URI baseURI;
        URI uri;
        String pattern;

        engine = testEngine();
        baseURI = URI.create("http://somehost.somedomain.net/abc/def/xyz.html?a=b");
        for (char c = 'A'; c <= 'Z'; c++) {
            md5 = Resource.md5((byte) c);
            md5str = Hex.encodeString(md5);
            label = new Label("logo.png", md5str + "/logo.png", md5);
            uri = engine.calculateURL(label, baseURI);
            pattern = "http://s[1-2]\\.uicdn\\.net/m1/" + md5str + "/logo\\.png";
            assertTrue(uri.toString().matches(pattern));
        }
    }

    private RewriteEngine testEngine() {
        RewriteEngine engine;

        engine = new RewriteEngine(new Index());
        engine.add(URI.create("http://s1.uicdn.net/m1/"));
        engine.add(URI.create("https://s1.uicdn.net/m1/"));
        engine.add(URI.create("http://s2.uicdn.net/m1/"));
        engine.add(URI.create("https://s2.uicdn.net/m1/"));
        return engine;
    }

}
