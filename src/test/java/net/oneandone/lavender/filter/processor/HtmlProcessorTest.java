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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

public class HtmlProcessorTest {

    protected StringWriter out;
    protected Processor processor;

    @BeforeEach
    public void setUp() {
        RewriteEngine rewriteEngine = mock(RewriteEngine.class);
        when(rewriteEngine.rewrite(any(String.class), eq(URI.create("http://x.y.z")), anyString())).thenReturn("http://a.b.c");

        processor = new HtmlProcessor();
        processor.setRewriteEngine(rewriteEngine, URI.create("http://x.y.z"), "/");
        out = new StringWriter();
        processor.setWriter(out);
    }

    @Test
    public void testSimpleWithSingleQuotedAttributes() throws IOException {

        String input = "<html>abc!<body><img src = '/a/b/c' /><link rel = 'stylesheet' href='/x/y/z' />"
                + "<script type = 'text/javascript' src = '/js'></body></html>";
        String expected = "<html>abc!<body><img src = 'http://a.b.c' />"
                + "<link rel = 'stylesheet' href='http://a.b.c' /><script type = 'text/javascript' src = 'http://a.b.c'></body></html>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testHtml5Source() throws IOException {

        String input = "<source src='/x/y/z'/>";
        String expected = "<source src='http://a.b.c'/>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testHtml5SourceSet() throws IOException {

        String input = "<img src='/x/y/z' srcset='/x/y/z, /x/y/z 640w,\n /x/y/z 2.0x, data:AAAA'/>";
        String expected = "<img src='http://a.b.c' srcset='http://a.b.c, http://a.b.c 640w,\n http://a.b.c 2.0x, data:AAAA'/>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testHtml5OgImageMeta() throws IOException {
        String input = "<meta content='/x/y/z' property='og:image'/>";
        String expected = "<meta content='http://a.b.c' property='og:image'/>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testHtml5IgnoresNonOgImageMeta() throws IOException {
        String input = "<meta content='/x/y/z' />";
        String expected = "<meta content='/x/y/z' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testDataLavenderAttributes() throws IOException {
        String input = "<a src='/x/y/z' data-lavender-a='x/y/z' data-lavender-2='x/y/z' >";
        String output = "<a src='/x/y/z' data-lavender-a='http://a.b.c' data-lavender-2='http://a.b.c' >";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    @Test
    public void testAHrefAttributes() throws IOException {
        String input = "<a href='/x/y/z'>";
        String output = "<a href='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    @Test
    public void testFormActionAttributes() throws IOException {
        String input = "<form action='/x/y/z'>";
        String output = "<form action='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    @Test
    public void testIframeSourceAttributes() throws IOException {
        String input = "<iframe src='/x/y/z'>";
        String output = "<iframe src='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    @Test
    public void testDataOther() throws IOException {
        String input = "<foo src='/x/y/z' data-a='x/y/z' data='x/y/z' >";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(input, out.getBuffer().toString());
    }


    @Test
    public void testSimpleWithDoubleQuotedAttributes() throws IOException {

        String input = "<html>abc!<body><img  src  =  \"/a/b/c\" /><link rel=\"stylesheet\" href=\"/x/y/z\" />"
                + "<script type = \"text/javascript\" src = \"/js\"></body></html>";
        String expected = "<html>abc!<body><img  src  =  \"http://a.b.c\" />"
                + "<link rel=\"stylesheet\" href=\"http://a.b.c\" />"
                + "<script type = \"text/javascript\" src = \"http://a.b.c\"></body></html>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testSimpleWithUnquotedAttributes() throws IOException {

        String input = "<html>abc!<body><img src = abc /><link rel=stylesheet href=xyz />"
                + "<script type = text/javascript src = /js ></body></html>";
        String expected = "<html>abc!<body><img src = http://a.b.c />"
                + "<link rel=stylesheet href=http://a.b.c /><script type = text/javascript src = http://a.b.c ></body></html>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testSimpleWithSimilarAttributes() throws IOException {

        String input = "<img s sr src s='x' sr='y' srcx='z' src='/a/b/c' />";
        String expected = "<img s sr src s='x' sr='y' srcx='z' src='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testSimpleWithSimilarTags() throws IOException {

        String input = "<imgs src='/a/b/c' /><links href='/x/y/z' />";
        String expected = "<imgs src='/a/b/c' /><links href='/x/y/z' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testImgNormal() throws IOException {

        String input = "<img src='/a/b/c' /><links href='/x/y/z' />";
        String expected = "<img src='http://a.b.c' /><links href='/x/y/z' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testImgData() throws IOException {

        String input = "<img src='data:/a/b/c' /><links href='/x/y/z' />";
        String expected = "<img src='data:/a/b/c' /><links href='/x/y/z' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testObject() throws IOException {

        String input = "<object type='image/svg+xml' data='/a/b/c'>";
        String expected = "<object type='image/svg+xml' data='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testAllAtributes() throws IOException {

        String input = "<img href='aaa' rel='bbb' src='/a/b/c' type='ccc' style='ddd' zzz='zzz' />";
        String expected = "<img href='aaa' rel='bbb' src='http://a.b.c' type='ccc' style='ddd' zzz='zzz' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testDuplicateAttribute() throws IOException {

        String input = "<link type='text/css' href='/style/general/ie6.css' rel='stylesheet' type='text/css'>";
        String expected = "<link type='text/css' href='http://a.b.c' rel='stylesheet' type='text/css'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testOnlyImgSrc() throws IOException {

        String input = "<img /><img src = '/a/b/c' />";
        String expected = "<img /><img src = 'http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testOnlyLinkRelStylesheet() throws IOException {

        String input = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='stylesheet' href='/x/y/z' />";
        String expected = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='stylesheet' href='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testOnlyLinkRelIcon() throws IOException {

        String input = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='icon' href='/x/y/z' />";
        String expected = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='icon' href='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testOnlyLinkRelShortcutIcon() throws IOException {

        String input = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='shortcut icon' href='/x/y/z' />";
        String expected = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='shortcut icon' href='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }
    
    @Test
    public void testOnlyLinkRelPreload() throws IOException {

        String input = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='preload' href='/x/y/z' />";
        String expected = "<link rel='other' href='/x/y/z' /><link href='/x/y/z' /><link rel='preload' href='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testOnlyScriptTypeTextJavascript() throws IOException {

        String input = "<script type='other' src='/x/y/z' /><script src='/x/y/z' /><script type='text/javascript' src='/x/y/z' />";
        String expected = "<script type='other' src='/x/y/z' /><script src='http://a.b.c' /><script type='text/javascript' src='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testOnlyInputTypeImage() throws IOException {

        String input = "<input type='other' src='/x/y/z' /><input src='/x/y/z' /><input type='image' src='/x/y/z' />";
        String expected = "<input type='other' src='/x/y/z' /><input src='/x/y/z' /><input type='image' src='http://a.b.c' />";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testChunked() throws IOException {

        String input = "<html><body><img src=\"/a/b/c\" /><link rel='stylesheet' href=\"/x/y/z\" /></body></html>";
        String expected = "<html><body><img src=\"http://a.b.c\" /><link rel='stylesheet' href=\"http://a.b.c\" /></body></html>";

        processor.process(input, 0, 25);
        processor.process(input, 25, 25);
        processor.process(input, 50, 35);
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testNotFinished() throws IOException {

        String input = "<html><body><img src='/a/b/c' /><link href='/x/y/z";
        String expected = "<html><body><img src='http://a.b.c' /><link href='/x/y/z";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testComment() throws IOException {

        String input = "...<!-- \n //--><img ... src='/a/b/c'>...<link rel='stylesheet' href='/x/y/z'>...";
        String expected = "...<!-- \n //--><img ... src='http://a.b.c'>...<link rel='stylesheet' href='http://a.b.c'>...";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testSpecialCharsInComment() throws IOException {

        String input = "<script><!-- for(C=0;C<A.length;++C){}; - -> > --><img src=\"/a/b/c\">";
        String expected = "<script><!-- for(C=0;C<A.length;++C){}; - -> > --><img src=\"http://a.b.c\">";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testDoctype() throws IOException {
        String input = "<!DOCTYPE HTML ><img src='/a/b/c'>";
        String expected = "<!DOCTYPE HTML ><img src='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testCData() throws IOException {
        String input = "<![CDATA[ <img src='/a/b/c'> ]]><img src='/a/b/c'>";
        String expected = "<![CDATA[ <img src='/a/b/c'> ]]><img src='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testConditinalDownlevelHidden() throws IOException {
        String input = "<!--[if IE6]><img src='/a/b/c'><![endif]--><img src='/a/b/c'>";
        String expected = "<!--[if IE6]><img src='http://a.b.c'><![endif]--><img src='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testConditinalDownlevelRevealed() throws IOException {
        String input = "<![if !IE]><img src='/a/b/c'><![endif><img src='/a/b/c'>";
        String expected = "<![if !IE]><img src='http://a.b.c'><![endif><img src='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testConditinalDownlevelRevealed2() throws IOException {
        String input = "<!--[if !IE]><!--><img src='/a/b/c'><!--<![endif]--><img src='/a/b/c'>";
        String expected = "<!--[if !IE]><!--><img src='http://a.b.c'><!--<![endif]--><img src='http://a.b.c'>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    @Disabled("invalid test")
    public void testEscapedSingleQuotes() throws IOException {

        String input = "...<img alt=' \\' \" \" ' src='/a/b/c'>...";
        String expected = "...<img alt=' \\' \" \" ' src='http://a.b.c'>...";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    @Disabled("invalid test")
    public void testEscapedDoubleQuotes() throws IOException {

        String input = "...<img alt=\" ' \\' \\\" \\\" \" src=\"/a/b/c\">...";
        String expected = "...<img alt=\" ' \\' \\\" \\\" \" src=\"http://a.b.c\">...";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testEmbeddedCss() throws IOException {

        String input = "<html><div class='x' style='abc url(/a/b/c);'>...";
        String expected = "<html><div class='x' style='abc url(http://a.b.c);'>...";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    public void testEmbeddedCss2() throws IOException {

        String input = "<html><dd class='x' style='abc url(/a/b/c);'>...";
        String expected = "<html><dd class='x' style='abc url(http://a.b.c);'>...";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }
}
