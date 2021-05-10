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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndexTest {
    private static final World WORLD = World.createMinimal();

    private FileNode indexFile;
    private Index index;

    @BeforeEach
    public void setup() throws IOException {
        indexFile = WORLD.getTemp().createTempFile();
        index = new Index();
        index.add(new Label("img/close.gif", "app/ABCDEF1234567890-close.gif", Hex.decodeString("abcdef1234567890")));
        index.save(indexFile);
    }

    @Test
    public void testWriteToExistingFile() throws IOException {
        Label l1 = new Label("img/test.png", "app/ABCDEF1234567890-test.png", Hex.decodeString("ABCDEF1234567890"));
        Label l2 = new Label("modules/stageassistent/img/test.gif", "stageassistent/ABCDEF1234567890-test.gif",
                Hex.decodeString("ABCDEF1234567890"));
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

        Label l1 = new Label("img/test.png", "app/ABCDEF1234567890-test.png", Hex.decodeString("ABCDEF1234567890"));
        Label l2 = new Label("modules/stageassistent/img/test.gif", "stageassistent/ABCDEF1234567890-test.gif",
                Hex.decodeString("ABCDEF1234567890"));
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
    public void testLookup() {
        Label label = index.lookup("img/close.gif");
        assertNotNull(label);
        assertEquals("app/ABCDEF1234567890-close.gif", label.getLavendelizedPath());
        assertEquals("abcdef1234567890", Hex.encodeString(label.md5()));
    }

    @Test
    public void testLookupNonExistingRef() {
        assertNull(index.lookup("img/nonexisting.gif"));
    }

    @Test
    public void testPropertiesConstructor() throws Exception {
        Index.load(indexFile);
    }

    @Test()
    public void testPropertiesConstructorNonExistingFile() throws Exception {
        assertThrows(IOException.class, () -> {
            Index.load(indexFile.join("file:///nosuchfile"));
        });
    }

    @Test()
    public void testPropertiesConstructorCorruptPropertiesFile() throws Exception {
        indexFile.writeString("\\u00");
        assertThrows(RuntimeException.class, () -> {
            Index.load(indexFile);
        });
    }

}
