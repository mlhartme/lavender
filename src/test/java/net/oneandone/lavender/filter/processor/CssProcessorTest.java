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

import net.oneandone.sushi.fs.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CssProcessorTest {
    private static final World WORLD = World.createMinimal();

    protected StringWriter out;
    protected CssProcessor processor;
    protected RewriteEngine rewriteEngine;

    @BeforeEach
    public void setUp() {
        rewriteEngine = mock(RewriteEngine.class);
        when(rewriteEngine.rewrite(eq("/x/y/z.gif"), eq(URI.create("http://x.y.z")), anyString())).thenReturn("http://a.b.c");

        processor = new CssProcessor();
        processor.setRewriteEngine(rewriteEngine, URI.create("http://x.y.z"), "/");
        out = new StringWriter();
        processor.setWriter(out);

    }

    @Test
    public void testSimple() throws IOException {

        String input = "background: transparent url(/x/y/z.gif) no-repeat top left;";
        String expected = "background: transparent url(http://a.b.c) no-repeat top left;";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testWhiteSpace() throws IOException {

        String input = "background: transparent url( /x/y/z.gif ) no-repeat top left;";
        String expected = "background: transparent url( http://a.b.c ) no-repeat top left;";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testEscapedParenthesis() throws IOException {
        when(rewriteEngine.rewrite(eq("/x/y/z(2).gif"), eq(URI.create("http://x.y.z")), anyString())).thenReturn("http://a.b.c(2)");

        String input = "background: transparent url( /x/y/z\\(2\\).gif ) no-repeat top left;";
        String expected = "background: transparent url( http://a.b.c(2) ) no-repeat top left;";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testSingleQuote() throws IOException {

        String input = "background: transparent url('/x/y/z.gif') no-repeat top left;";
        String expected = "background: transparent url('http://a.b.c') no-repeat top left;";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testDoubleQuote() throws IOException {

        String input = "background: transparent url(\"/x/y/z.gif\") no-repeat top left;";
        String expected = "background: transparent url(\"http://a.b.c\") no-repeat top left;";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testSingleAndDoubleQuote() throws IOException {
        when(rewriteEngine.rewrite(eq("'/x/y/z.gif\""), eq(URI.create("http://x.y.z")), anyString())).thenReturn("'/x/y/z.gif\"");

        String input = "background: transparent url('/x/y/z.gif\") no-repeat top left;";
        String expected = "background: transparent url('/x/y/z.gif\") no-repeat top left;";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testInlineSvg() throws IOException {
        when(rewriteEngine.rewrite(eq("'/x/y/z.gif\""), eq(URI.create("http://x.y.z")), anyString())).thenReturn("'/x/y/z.gif\"");

        String input = "background-image: url('data:image/svg+xml;utf8,<svg width=\"16\" height=\"16\" viewBox=\"0 0 12 12\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"><polygon points=\"2,2 5,2 5,3 3,3 3,9 9,9 9,7 10,7 10,10 2,10\"/><polygon points=\"6.2,2 10,2 10,5.79 8.58,4.37 6.5,6.5 5.5,5.5 7.6,3.4\"/></svg>');";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(input, out.getBuffer().toString());
    }

    @Test
    public void testNotFinished() throws IOException {

        String input = "background: transparent url(/x/y/z.gif";
        String expected = "background: transparent url(/x/y/z.gif";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testComplex() throws IOException {
        when(rewriteEngine.rewrite(any(String.class), eq(URI.create("http://x.y.z")), anyString())).thenReturn("http://a.b.c");

        String input;
        String expected;

        input = WORLD.resource("CssProcessorTest.css").readString();
        processor.process(input, 0, input.length());
        processor.flush();
        expected = WORLD.resource("CssProcessorTest-expected.css").readString();
        assertEquals(expected, out.getBuffer().toString());
    }

}
