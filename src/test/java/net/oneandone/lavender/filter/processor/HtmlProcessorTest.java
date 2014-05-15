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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlProcessorTest {

    protected StringWriter out;
    protected Processor processor;

    @Before
    public void setUp() throws Exception {
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
    public void testIsolatedObjectParam() throws IOException {
        String input = "<object/><param name='movie' value='/x/y/z'/>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(input, out.getBuffer().toString());
    }

    @Test
    public void testObjectParam() throws IOException {
        String input = "<object><param name='movie' value='/x/y/z'/></object>";
        String output = "<object><param name='movie' value='http://a.b.c'/></object>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    @Test
    public void testFlash() throws IOException {
        String input = "      <div id=\"flashContent\">\n" +
                "        <object classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\" width=\"450\" height=\"450\" id=\"nHosting-apps-wuerfelani_v01\" align=\"middle\">\n" +
                "          <param name=\"movie\" value=\"/x/y/z\" />\n" +
                "          <param name=\"quality\" value=\"best\" />\n" +
                "          <param name=\"bgcolor\" value=\"#2e86c7\" />\n" +
                "          <param name=\"play\" value=\"true\" />\n" +
                "          <param name=\"loop\" value=\"false\" />\n" +
                "          <param name=\"wmode\" value=\"transparent\" />\n" +
                "          <param name=\"scale\" value=\"showall\" />\n" +
                "          <param name=\"menu\" value=\"false\" />\n" +
                "          <param name=\"devicefont\" value=\"false\" />\n" +
                "          <param name=\"salign\" value=\"\" />\n" +
                "          <param name=\"allowScriptAccess\" value=\"sameDomain\" />\n" +
                "          <!--[if !IE]>-->\n" +
                "          <object type=\"application/x-shockwave-flash\" data=\"/x/y/z\" width=\"450\" height=\"450\">\n" +
                "            <param name=\"movie\" value=\"/x/y/z\" />\n" +
                "            <param name=\"quality\" value=\"best\" />\n" +
                "            <param name=\"bgcolor\" value=\"#2e86c7\" />\n" +
                "            <param name=\"play\" value=\"true\" />\n" +
                "            <param name=\"loop\" value=\"false\" />\n" +
                "            <param name=\"wmode\" value=\"transparent\" />\n" +
                "            <param name=\"scale\" value=\"showall\" />\n" +
                "            <param name=\"menu\" value=\"false\" />\n" +
                "            <param name=\"devicefont\" value=\"false\" />\n" +
                "            <param name=\"salign\" value=\"\" />\n" +
                "            <param name=\"allowScriptAccess\" value=\"sameDomain\" />\n" +
                "            <!--<![endif]-->\n" +
                "            <a href=\"http://www.adobe.com/go/getflash\">\n" +
                "              <img src=\"http://www.adobe.com/images/shared/download_buttons/get_flash_player.gif\" alt=\"Get Adobe Flash Player\" />\n" +
                "            </a>\n" +
                "            <!--[if !IE]>-->\n" +
                "          </object>\n" +
                "          <!--<![endif]-->\n" +
                "        </object>\n" +
                "      </div>\n";
        String output = "      <div id=\"flashContent\">\n" +
                "        <object classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\" width=\"450\" height=\"450\" id=\"nHosting-apps-wuerfelani_v01\" align=\"middle\">\n" +
                "          <param name=\"movie\" value=\"http://a.b.c\" />\n" +
                "          <param name=\"quality\" value=\"best\" />\n" +
                "          <param name=\"bgcolor\" value=\"#2e86c7\" />\n" +
                "          <param name=\"play\" value=\"true\" />\n" +
                "          <param name=\"loop\" value=\"false\" />\n" +
                "          <param name=\"wmode\" value=\"transparent\" />\n" +
                "          <param name=\"scale\" value=\"showall\" />\n" +
                "          <param name=\"menu\" value=\"false\" />\n" +
                "          <param name=\"devicefont\" value=\"false\" />\n" +
                "          <param name=\"salign\" value=\"\" />\n" +
                "          <param name=\"allowScriptAccess\" value=\"sameDomain\" />\n" +
                "          <!--[if !IE]>-->\n" +
                "          <object type=\"application/x-shockwave-flash\" data=\"http://a.b.c\" width=\"450\" height=\"450\">\n" +
                "            <param name=\"movie\" value=\"http://a.b.c\" />\n" +
                "            <param name=\"quality\" value=\"best\" />\n" +
                "            <param name=\"bgcolor\" value=\"#2e86c7\" />\n" +
                "            <param name=\"play\" value=\"true\" />\n" +
                "            <param name=\"loop\" value=\"false\" />\n" +
                "            <param name=\"wmode\" value=\"transparent\" />\n" +
                "            <param name=\"scale\" value=\"showall\" />\n" +
                "            <param name=\"menu\" value=\"false\" />\n" +
                "            <param name=\"devicefont\" value=\"false\" />\n" +
                "            <param name=\"salign\" value=\"\" />\n" +
                "            <param name=\"allowScriptAccess\" value=\"sameDomain\" />\n" +
                "            <!--<![endif]-->\n" +
                "            <a href=\"http://a.b.c\">\n" +
                "              <img src=\"http://a.b.c\" alt=\"Get Adobe Flash Player\" />\n" +
                "            </a>\n" +
                "            <!--[if !IE]>-->\n" +
                "          </object>\n" +
                "          <!--<![endif]-->\n" +
                "        </object>\n" +
                "      </div>\n";


        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());

    }


    // CHECKSTYLE:OFF
    @Test
    public void testFlashWithFlv() throws IOException {
        // test html from Mobile Order, Andreas Martin
        String input = "<object id='video' width='640' height='405' align='middle' autostart='true' codebase='https://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,0,0' classid='clsid:d27cdb6e-ae6d-11cf-96b8-444553540000'>\n" +
                "<param value='sameDomain' name='allowScriptAccess'>\n" +
                "<param value='false' name='allowFullScreen'>\n" +
                "<param value='/img/pages/all-net-flat/flash/1und1Player.swf?flvsource=/img/pages/all-net-flat/flash/2014-03-All-Net-Flat-11sec-final-V2.flv&skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf' name='movie'>\n" +
                "<param value='high' name='quality'>\n" +
                "<param value='ffffff' name='bgcolor'>\n" +
                "<param value='transparent' name='wmode'>\n" +
                "<param value='true' name='allowfullscreen'>\n" +
                "<param value='true' name='autoplay'>\n" +
                "<embed width='640' height='405' align='middle' pluginspage='https://www.macromedia.com/go/getflashplayer'\n"
                     + " type='application/x-shockwave-flash' allowscriptaccess='sameDomain' autostart='true' name='video'\n"
                     + " autoplay='true' allowfullscreen='true' wmode='transparent' bgcolor='ffffff' quality='high'\n"
                     + " src='/img/pages/all-net-flat/flash/1und1Player.swf?flvsource=/img/pages/all-net-flat/flash/2014-03-All-Net-Flat-11sec-final-V2.flv&skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf'>\n" +
                "</object>";
        String output = "<object id='video' width='640' height='405' align='middle' autostart='true' codebase='https://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,0,0' classid='clsid:d27cdb6e-ae6d-11cf-96b8-444553540000'>\n" +
                "<param value='sameDomain' name='allowScriptAccess'>\n" +
                "<param value='false' name='allowFullScreen'>\n" +
                "<param value='http://a.b.c?flvsource=http://a.b.c&skinsource=http://a.b.c' name='movie'>\n" +
                "<param value='high' name='quality'>\n" +
                "<param value='ffffff' name='bgcolor'>\n" +
                "<param value='transparent' name='wmode'>\n" +
                "<param value='true' name='allowfullscreen'>\n" +
                "<param value='true' name='autoplay'>\n" +
                "<embed width='640' height='405' align='middle' pluginspage='https://www.macromedia.com/go/getflashplayer'\n"
                + " type='application/x-shockwave-flash' allowscriptaccess='sameDomain' autostart='true' name='video'\n"
                + " autoplay='true' allowfullscreen='true' wmode='transparent' bgcolor='ffffff' quality='high'\n"
                + " src='/img/pages/all-net-flat/flash/1und1Player.swf?flvsource=/img/pages/all-net-flat/flash/2014-03-All-Net-Flat-11sec-final-V2.flv&skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf'>\n" +
                "</object>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    @Test
    public void testFlashWithFlv2() throws IOException {
        String input = "<object classid='clsid:d27cdb6e-ae6d-11cf-96b8-444553540000' \n" +
                "codebase='https://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,0,0' \n" +
                "id='video' width='640' height='405' autostart='true' align='middle'>\n" +
                "  <param name='allowScriptAccess' value='sameDomain'>\n" +
                "  <param name='allowFullScreen' value='false'>\n" +
                "  <param name='movie' value='/img/pages/all-net-flat/flash/1und1Player.swf?flvsource=/img/pages/all-net-flat/flash/2014-03-All-Net-Flat-11sec-final-V2.flv&amp;skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf'>\n" +
                "  <param name='quality' value='high'>\n" +
                "  <param name='bgcolor' value='ffffff'>\n" +
                "  <param name='wmode' value='transparent'>\n" +
                "  <param name='allowfullscreen' value='true'>\n" +
                "  <param name='autoplay' value='true'>\n" +
                "  <embed src='/img/pages/all-net-flat/flash/1und1Player.swf?flvsource=/img/pages/all-net-flat/flash/2014-03-All-Net-Flat-11sec-final-V2.flv&amp;skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf' quality='high' bgcolor='ffffff' wmode='transparent' allowfullscreen='true' autoplay='true' name='video' width='640' height='405' autostart='true' align='middle' allowscriptaccess='sameDomain' type='application/x-shockwave-flash' pluginspage='https://www.macromedia.com/go/getflashplayer'>\n" +
                "</object>";
        String output = "<object classid='clsid:d27cdb6e-ae6d-11cf-96b8-444553540000' \n" +
                "codebase='https://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,0,0' \n" +
                "id='video' width='640' height='405' autostart='true' align='middle'>\n" +
                "  <param name='allowScriptAccess' value='sameDomain'>\n" +
                "  <param name='allowFullScreen' value='false'>\n" +
                "  <param name='movie' value='http://a.b.c?flvsource=http://a.b.c&amp;skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf'>\n" +
                "  <param name='quality' value='high'>\n" +
                "  <param name='bgcolor' value='ffffff'>\n" +
                "  <param name='wmode' value='transparent'>\n" +
                "  <param name='allowfullscreen' value='true'>\n" +
                "  <param name='autoplay' value='true'>\n" +
                "  <embed src='/img/pages/all-net-flat/flash/1und1Player.swf?flvsource=/img/pages/all-net-flat/flash/2014-03-All-Net-Flat-11sec-final-V2.flv&amp;skinsource=/img/pages/all-net-flat/flash/1und1Skin.swf' quality='high' bgcolor='ffffff' wmode='transparent' allowfullscreen='true' autoplay='true' name='video' width='640' height='405' autostart='true' align='middle' allowscriptaccess='sameDomain' type='application/x-shockwave-flash' pluginspage='https://www.macromedia.com/go/getflashplayer'>\n" +
                "</object>";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(output, out.getBuffer().toString());
    }

    // CHECKSTYLE:ON

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
    public void testOnlyScriptTypeTextJavascript() throws IOException {

        String input = "<script type='other' src='/x/y/z' /><script src='/x/y/z' /><script type='text/javascript' src='/x/y/z' />";
        String expected = "<script type='other' src='/x/y/z' /><script src='/x/y/z' /><script type='text/javascript' src='http://a.b.c' />";

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
    @Ignore("invalid test")
    public void testEscapedSingleQuotes() throws IOException {

        String input = "...<img alt=' \\' \" \" ' src='/a/b/c'>...";
        String expected = "...<img alt=' \\' \" \" ' src='http://a.b.c'>...";

        processor.process(input, 0, input.length());
        processor.flush();

        assertEquals(expected, out.getBuffer().toString());
    }

    @Test
    @Ignore("invalid test")
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
