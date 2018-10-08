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
package net.oneandone.lavender.filter;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class WriterOutputStreamTest {
    private static final String UTF_8 = "UTF-8";

    protected WriterOutputStream wos;
    protected StringWriter result;

    @Before
    public void setup() {
        result = new StringWriter();
        wos = WriterOutputStream.create(result, UTF_8);
    }

    private void check(String str) throws IOException {
        result.getBuffer().setLength(0);
        wos.write(str.getBytes());
        assertEquals(str, result.toString());
    }

    @Test
    public void testWriteByteArray() throws IOException {
        check("");
        check("\u00e4\u00f6\u00fc");
        check("abc");
        check("xyz");
    }

    @Test
    public void testWriteByteArraySplitted() throws IOException {
        /*
         * German umlauts äöü as UTF-8 encoded byte[] is six bytes. Write the six bytes in two chunks of three bytes
         * each. The third byte of the first chunk can't be processed immediately. Verify the splitted bytes are
         * assembled correctly to characters.
         */
        byte[] bytes = "\u00e4\u00f6\u00fc".getBytes(StandardCharsets.UTF_8);
        assertEquals(6, bytes.length);
        byte[] b1 = new byte[3];
        byte[] b2 = new byte[3];
        System.arraycopy(bytes, 0, b1, 0, 3);
        System.arraycopy(bytes, 3, b2, 0, 3);

        // write first chunk, only 2 bytes can be assembled to an character
        wos.write(b1);
        assertEquals("\u00e4", result.toString());
        result.getBuffer().setLength(0);

        // write second chunk, the 3rd byte of the first chunk could now be processed
        wos.write(b2);
        assertEquals("\u00f6\u00fc", result.toString());
    }

    @Test
    public void testWriteByteArrayOffLen() throws IOException {
        wos.write("\u00e4\u00f6\u00fc".getBytes(StandardCharsets.UTF_8), 2, 2);
        assertEquals("\u00f6", result.toString());
        result.getBuffer().setLength(0);

        wos.write("abc".getBytes(StandardCharsets.UTF_8), 0, 1);
        assertEquals("a", result.toString());
        result.getBuffer().setLength(0);

        wos.write("xyz".getBytes(StandardCharsets.UTF_8), 1, 2);
        assertEquals("yz", result.toString());
        result.getBuffer().setLength(0);

        wos.write("".getBytes(StandardCharsets.UTF_8), 0, 0);
        assertEquals("", result.toString());
        result.getBuffer().setLength(0);
    }

    @Test
    public void testWriteInt() throws IOException {

        wos.write(65);
        assertEquals("A", result.toString());
        wos.write(0);
    }

    @Test
    public void testWriteHuge() throws IOException {
        byte[] bytes = new byte[1000000];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 65;
        }

        wos.write(bytes, 5, 999990);
        assertEquals(999990, result.toString().length());
        assertEquals('A', result.toString().charAt(555555));
    }

    @Test
    public void testFlush() throws IOException {
        wos.flush();
    }

    @Test
    public void testClose() throws IOException {
        wos.close();
    }

    @Test
    public void testMalformedInput() throws IOException {
        wos.write("\u00e4ndern".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("\uFFFDndern", result.toString());
        result.getBuffer().setLength(0);
        wos.write("l\u00f6schen".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("l\uFFFDschen", result.toString());
    }
}
