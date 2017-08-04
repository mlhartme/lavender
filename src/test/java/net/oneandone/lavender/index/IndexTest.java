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
package net.oneandone.lavender.index;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IndexTest {
    private static final World WORLD = World.createMinimal();

    private FileNode indexFile;
    private Index index;

    @Before
    public void setup() throws IOException {
        indexFile = WORLD.getTemp().createTempFile();
        index = new Index();
        index.add(new Label("img/close.gif", "app/ABCDEF1234567890-close.gif", Hex.decode("abcdef1234567890".toCharArray())));
        index.save(indexFile);
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

        String readFileToString = indexFile.readString();
        assertTrue(readFileToString.contains("img/close.gif=app/ABCDEF1234567890-close.gif\\:abcdef1234567890"));
        assertTrue(readFileToString.contains("img/test.png=app/ABCDEF1234567890-test.png\\:abcdef1234567890"));
        assertTrue(readFileToString
                .contains("modules/stageassistent/img/test.gif=stageassistent/ABCDEF1234567890-test.gif\\:abcdef1234567890"));
    }

    @Test
    public void testWriteToNonExistingFile() throws IOException {
        FileNode nonexistingfile = WORLD.getTemp().createTempFile().deleteFile();
        assertFalse(nonexistingfile.exists());

        Index myIndex = new Index();

        Label l1 = new Label("img/test.png", "app/ABCDEF1234567890-test.png", Hex.decode("ABCDEF1234567890".toCharArray()));
        Label l2 = new Label("modules/stageassistent/img/test.gif", "stageassistent/ABCDEF1234567890-test.gif",
                Hex.decode("ABCDEF1234567890".toCharArray()));
        myIndex.add(l1);
        myIndex.add(l2);
        myIndex.save(nonexistingfile);

        assertTrue(nonexistingfile.exists());

        String readFileToString = nonexistingfile.readString();
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

    @Test
    public void testPropertiesConstructor() throws Exception {
        Index.load(indexFile);
    }

    @Test(expected = IOException.class)
    public void testPropertiesConstructorNonExistingFile() throws Exception {
        Index.load(indexFile.join("file:///nosuchfile"));
    }

    @Test(expected = RuntimeException.class)
    public void testPropertiesConstructorCorruptPropertiesFile() throws Exception {
        indexFile.writeString("\\u00");
        Index.load(indexFile);
    }

}
