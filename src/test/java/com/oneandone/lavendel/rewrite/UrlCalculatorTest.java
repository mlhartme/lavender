package com.oneandone.lavendel.rewrite;

import com.oneandone.lavendel.index.Label;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UrlCalculatorTest {

    private File nodesFile;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException, DecoderException {
        nodesFile = new File(tempFolder.getRoot(), "lavendel.nodes");
        String data = "";
        data += "http://s1.uicdn.net/m1" + IOUtils.LINE_SEPARATOR;
        data += "https://s1.uicdn.net/m1" + IOUtils.LINE_SEPARATOR;
        data += "http://s2.uicdn.net/m1/" + IOUtils.LINE_SEPARATOR;
        data += "https://s2.uicdn.net/m1/" + IOUtils.LINE_SEPARATOR;
        data += IOUtils.LINE_SEPARATOR;
        FileUtils.writeStringToFile(nodesFile, data);
    }

    @Test
    public void testConstructor() throws IOException {
        UrlCalculator urlCalculator = new UrlCalculator(nodesFile.toURI().toURL());
        assertNotNull(urlCalculator.httpNodes);
        assertEquals(2, urlCalculator.httpNodes.size());
        assertTrue(urlCalculator.httpNodes.keySet().contains("s1.uicdn.net"));
        assertTrue(urlCalculator.httpNodes.keySet().contains("s2.uicdn.net"));
        assertNotNull(urlCalculator.httpsNodes);
        assertEquals(2, urlCalculator.httpsNodes.size());
        assertTrue(urlCalculator.httpsNodes.keySet().contains("s1.uicdn.net"));
        assertTrue(urlCalculator.httpsNodes.keySet().contains("s2.uicdn.net"));
    }

    @Test
    public void testCalculateURL() throws IOException {
        UrlCalculator urlCalculator = new UrlCalculator(nodesFile.toURI().toURL());
        for (char c = 'A'; c <= 'Z'; c++) {
            byte[] md5 = DigestUtils.md5("" + c);
            String md5hex = DigestUtils.md5Hex("" + c);

            Label label = mock(Label.class);
            when(label.getLavendelizedPath()).thenReturn(md5hex + ".png");
            when(label.getOriginalPath()).thenReturn("logo.png");
            when(label.md5()).thenReturn(md5);

            URI baseURI = URI.create("http://somehost.somedomain.net/abc/def/xyz.html?a=b");
            URI uri = urlCalculator.calculateURL(label, baseURI);
            String pattern = "http://s[1-2]\\.uicdn\\.net/m1/" + md5hex + "\\.png";
            assertTrue(uri.toString().matches(pattern));
        }
    }
}
