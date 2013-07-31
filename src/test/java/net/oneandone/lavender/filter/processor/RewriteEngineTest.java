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
import net.oneandone.lavender.modules.Resource;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RewriteEngineTest {
    private RewriteEngine engine;

    @Before
    public void setUp() {
        Index index;

        index = new Index();
        index.add(new Label("in.jpg", "out.jpg", Resource.md5()));
        engine = new RewriteEngine(index);
        engine.add(URI.create("http://s1.cdn.net/"));
        engine.add(URI.create("http://s2.cdn.net/"));
    }

    @Test
    public void rewriteNormal() {
        assertEquals("http://s1.cdn.net/out.jpg", engine.rewrite("in.jpg", URI.create("http://localhost:80"), "/"));
    }

    @Test
    public void rewriteQuoted() {
        assertEquals("http://s1.cdn.net/out.jpg", engine.rewrite("'in.jpg'", URI.create("http://localhost:80"), "/"));
    }

    @Test
    public void rewriteAbsolute() {
        assertEquals("http://s1.cdn.net/out.jpg", engine.rewrite("http://localhost:80/in.jpg", URI.create("http://localhost:80"), "/"));
    }

    @Test
    public void rewriteImplicitProtocol() {
        assertEquals("http://s1.cdn.net/out.jpg", engine.rewrite("//localhost:80/in.jpg", URI.create("http://localhost:80"), "/"));
    }

    @Test
    public void rewriteNotFound() {
        assertEquals("unknown.jpg", engine.rewrite("unknown.jpg", URI.create("http://localhost:80"), "/"));
    }

    @Test
    public void rewriteInvalidUri() {
        // rewrite something where new URI(something) throws an exception
        assertEquals("http:", engine.rewrite("http:", URI.create("http://localhost:80/app/img/"), "/app/"));
    }

    @Test
    public void noRewriteOfAbsoulteURI() {
        URI reference = URI.create("http://x.y.z:1234/index.html");
        URI baseURI = URI.create("");
        String contextPath = "/";

        URI uri = engine.rewrite(reference, baseURI, contextPath);
        assertSame(reference, uri);
    }


    //-- resolve (an important helper method for rewrite)

    @Test
    public void resolveNormal() {
        assertEquals("img/close.gif", doResolve("img/close.gif", "http://localhost:80", "/"));
    }

    @Test
    public void resolveNullPath() {
        assertNull(doResolve("mailto:michael.hartmeier@1und1.de", "http://localhost:80", "/"));
    }

    @Test
    public void resolveRelativeReferenceRootcontext() {
        assertEquals("img/close.gif", doResolve("img/close.gif", "http://localhost:80/", "/"));
    }

    @Test
    public void resolveAbsoluteReferenceRootContext() {
        assertEquals("img/close.gif", doResolve("/img/close.gif", "http://localhost:80/", "/"));
    }

    @Test
    public void resolveRelativeReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("img/close.gif", "http://localhost:80/app/", "/app/"));
    }

    @Test
    public void resolveAbsoluteReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("/app/img/close.gif", "http://localhost:80/app/", "/app/"));
    }

    @Test
    public void resolveChildRelativeReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("close.gif", "http://localhost:80/app/img/", "/app/"));
    }

    @Test
    public void resolveParentRelativeReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("../img/close.gif", "http://localhost:80/app/img/", "/app/"));
    }

    private String doResolve(String reference, String baseUri, String contextPath) {
        return engine.resolve(URI.create(reference), URI.create(baseUri), contextPath);
    }


    //-- calculate URL

    @Test
    public void calculateURL() {
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
