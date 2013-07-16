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
package net.oneandone.lavender.processor;

import net.oneandone.lavender.rewrite.RewriteEngine;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CssProcessorTest {

    protected StringWriter out;
    protected CssProcessor processor;

    @Before
    public void setUp() throws Exception {
        RewriteEngine rewriteEngine = mock(RewriteEngine.class);
        when(rewriteEngine.rewrite(any(String.class), eq(URI.create("http://x.y.z")), anyString())).thenReturn("http://a.b.c");

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
    public void testNotFinished() throws IOException {

        String input = "background: transparent url(/x/y/z.gif";
        String expected = "background: transparent url(/x/y/z.gif";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testComplex() throws IOException {

        InputStream in = getClass().getResourceAsStream("/CssProcessorTest.css");
        String input = IOUtils.toString(in, "UTF-8");

        processor.process(input, 0, input.length());
        processor.flush();

        in = getClass().getResourceAsStream("/CssProcessorTest-expected.css");
        String expected = IOUtils.toString(in, "UTF-8");
        assertEquals(expected, out.getBuffer().toString());
    }

}
