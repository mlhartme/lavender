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
package net.oneandone.lavender.filter.processor;

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.index.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LavenderRewriteEngineTest {
    private LavenderRewriteEngine engine;

    @BeforeEach
    public void setUp() {
        Index index;

        index = new Index();
        index.add(new Label("in.jpg", "out.jpg", Util.md5()));
        engine = new LavenderRewriteEngine(index);
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
    public void rewritePreservesQueryParameter() {
        assertEquals("http://s1.cdn.net/out.jpg?param=1", engine.rewrite("http://localhost:80/in.jpg?param=1", URI.create("http://localhost:80"), "/"));
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


    //-- calculate URL

    @Test
    public void calculateURL() {
        byte[] md5;
        String md5str;
        Label label;
        URI baseURI;
        URI uri;
        String pattern;

        LavenderRewriteEngine rewriteStrategy = testRewriteStrategy();
        baseURI = URI.create("http://somehost.somedomain.net/abc/def/xyz.html?a=b");
        for (char c = 'A'; c <= 'Z'; c++) {
            md5 = Util.md5((byte) c);
            md5str = Hex.encodeString(md5);
            label = new Label("logo.png", md5str + "/logo.png", md5);
            uri = rewriteStrategy.calculateURL(label, baseURI);
            pattern = "http://s[1-2]\\.uicdn\\.net/m1/" + md5str + "/logo\\.png";
            assertTrue(uri.toString().matches(pattern));
        }
    }

    @Test
    public void nodeHostWithPort() {
        LavenderRewriteEngine engine = new LavenderRewriteEngine(new Index());
        engine.add(URI.create("http://s1.uicdn.net:8080/m1/"));

        byte[] md5 = Util.md5("content".getBytes());
        String md5str = Hex.encodeString(md5);
        Label label = new Label("logo.png", md5str + "/logo.png", md5);

        URI baseURI = URI.create("http://host.net");
        URI uri = engine.calculateURL(label, baseURI);

        assertEquals("http://s1.uicdn.net:8080/m1/9a0364b9e99bb480dd25e1f0284c8555/logo.png", uri.toString());
    }

    private LavenderRewriteEngine testRewriteStrategy() {
        LavenderRewriteEngine rewriteStrategy = new LavenderRewriteEngine(new Index());

        rewriteStrategy.add(URI.create("http://s1.uicdn.net/m1/"));
        rewriteStrategy.add(URI.create("https://s1.uicdn.net/m1/"));
        rewriteStrategy.add(URI.create("http://s2.uicdn.net/m1/"));
        rewriteStrategy.add(URI.create("https://s2.uicdn.net/m1/"));
        return rewriteStrategy;
    }

}
