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
package net.oneandone.lavendel.index;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IndexTest {
    private File indexFile;
    private Index index;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        indexFile = new File(tempFolder.getRoot(), "index.idx");
        String data = "";
        data += "img/close.gif=app/ABCDEF1234567890-close.gif\\:abcdef1234567890";
        data += IOUtils.LINE_SEPARATOR;
        FileUtils.writeStringToFile(indexFile, data);
        index = new Index(indexFile);
    }

    @Test
    public void testWriteToExistingFile() throws IOException {
        Label l1 = new Label("img/test.png", "app/ABCDEF1234567890-test.png", Hex.decode("ABCDEF1234567890".toCharArray()));
        Label l2 = new Label("modules/stageassistent/img/test.gif", "stageassistent/ABCDEF1234567890-test.gif",
                Hex.decode("ABCDEF1234567890".toCharArray()));
        index.add(l1);
        index.add(l2);
        index.save(indexFile);

        assertTrue(indexFile.exists());

        String readFileToString = FileUtils.readFileToString(indexFile);
        assertTrue(readFileToString.contains("img/close.gif=app/ABCDEF1234567890-close.gif\\:abcdef1234567890"));
        assertTrue(readFileToString.contains("img/test.png=app/ABCDEF1234567890-test.png\\:abcdef1234567890"));
        assertTrue(readFileToString
                .contains("modules/stageassistent/img/test.gif=stageassistent/ABCDEF1234567890-test.gif\\:abcdef1234567890"));
    }

    @Test
    public void testWriteToNonExistingFile() throws IOException {
        File nonexistingfile = new File(tempFolder.getRoot(), "nonexistingfile.idx");
        assertFalse(nonexistingfile.exists());

        Index myIndex = new Index();

        Label l1 = new Label("img/test.png", "app/ABCDEF1234567890-test.png", Hex.decode("ABCDEF1234567890".toCharArray()));
        Label l2 = new Label("modules/stageassistent/img/test.gif", "stageassistent/ABCDEF1234567890-test.gif",
                Hex.decode("ABCDEF1234567890".toCharArray()));
        myIndex.add(l1);
        myIndex.add(l2);
        myIndex.save(nonexistingfile);

        assertTrue(nonexistingfile.exists());

        String readFileToString = FileUtils.readFileToString(nonexistingfile);
        assertTrue(readFileToString.contains("img/test.png=app/ABCDEF1234567890-test.png\\:abcdef1234567890"));
        assertTrue(readFileToString
                .contains("modules/stageassistent/img/test.gif=stageassistent/ABCDEF1234567890-test.gif\\:abcdef1234567890"));
    }

    @Test
    public void testLookup() throws IOException {
        Label label = index.lookup("img/close.gif");
        assertNotNull(label);
        assertEquals("app/ABCDEF1234567890-close.gif", label.getLavendelizedPath());
        assertEquals("abcdef1234567890", Hex.encodeString(label.md5()));
    }

    @Test
    public void testLookupNonExistingRef() throws IOException {
        assertNull(index.lookup("img/nonexisting.gif"));
    }

    @Test(expected = FileNotFoundException.class)
    public void testPropertiesConstructorNonExistingFile() throws Exception {
        new Index(new URL("file:///nosuchfile"));
    }

    @Test(expected = RuntimeException.class)
    public void testPropertiesConstructorCorruptPropertiesFile() throws Exception {
        FileUtils.writeStringToFile(indexFile, "\\u00");
        new Index(indexFile.toURI().toURL());
    }

    @Test
    public void testPropertiesConstructor() throws Exception {
        new Index(indexFile.toURI().toURL());
    }

    @Test
    public void testInputStreamConstructor() throws Exception {
        new Index(new FileInputStream(indexFile));
    }
}
