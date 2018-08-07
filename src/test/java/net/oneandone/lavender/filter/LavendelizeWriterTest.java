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

import net.oneandone.lavender.filter.processor.Processor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LavendelizeWriterTest {

    protected Writer writer;
    protected Processor processor;
    protected LavendelizeWriter lw;
    protected ReaderAnswer reader;

    @Before
    public void setup() throws IOException {
        processor = mock(Processor.class);

        reader = new ReaderAnswer();
        doAnswer(reader).when(processor).process(any(CharSequence.class), anyInt(), anyInt());

        writer = mock(Writer.class);

        LavendelizeHttpServletResponse response = mock(LavendelizeHttpServletResponse.class);
        when(response.initialize()).thenReturn(processor);
        lw = new LavendelizeWriter(processor);
    }

    @Test
    public void testWriteCharArray() throws IOException {

        lw.write("\u00e4\u00f6\u00fc".toCharArray());
        assertEquals("\u00e4\u00f6\u00fc", reader.toString());

        verify(processor, times(1)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("abc".toCharArray());
        assertEquals("abc", reader.toString());

        lw.write("xyz".toCharArray());
        assertEquals("xyz", reader.toString());

        verify(processor, times(3)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("".toCharArray());
        assertEquals("", reader.toString());

        verify(processor, times(4)).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testWriteInt() throws IOException {

        lw.write(65);
        assertEquals("A", reader.toString());

        verify(processor, times(1)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write(0);

        lw.write(Integer.MAX_VALUE);

        verify(processor, times(3)).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testWriteString() throws IOException {

        lw.write("\u00e4\u00f6\u00fc");
        assertEquals("\u00e4\u00f6\u00fc", reader.toString());

        verify(processor, times(1)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("abc");
        assertEquals("abc", reader.toString());

        lw.write("xyz");
        assertEquals("xyz", reader.toString());

        verify(processor, times(3)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("");

        verify(processor, times(4)).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testWriteCharArrayOffLen() throws IOException {

        lw.write("\u00e4\u00f6\u00fc".toCharArray(), 1, 1);
        assertEquals("\u00f6", reader.toString());

        verify(processor, times(1)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("abc".toCharArray(), 0, 1);
        assertEquals("a", reader.toString());

        lw.write("xyz".toCharArray(), 1, 2);
        assertEquals("yz", reader.toString());

        verify(processor, times(3)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("".toCharArray(), 0, 0);

        verify(processor, times(4)).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testWriteStringOffLen() throws IOException {

        lw.write("\u00e4\u00f6\u00fc", 1, 1);
        assertEquals("\u00f6", reader.toString());

        verify(processor, times(1)).process(any(CharSequence.class), anyInt(), anyInt());

        lw.write("abc", 0, 1);
        assertEquals("a", reader.toString());

        lw.write("xyz", 1, 2);
        assertEquals("yz", reader.toString());

        verify(processor, times(3)).process(any(CharSequence.class), anyInt(), anyInt());
        lw.write("", 0, 0);

        verify(processor, times(4)).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testWriteHuge() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000000; i++) {
            sb.append('A');
        }

        lw.write(sb.toString());
        assertEquals(1000000, reader.toString().length());
        assertEquals(sb.toString(), reader.toString());

        verify(processor, times(1)).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testFlush() throws IOException {
        lw.flush();
        verify(processor, times(1)).flush();
    }

    @Test
    public void testClose() throws IOException {
        lw.close();
        verify(processor, times(1)).close();
    }

    class ReaderAnswer implements Answer<String> {
        private CharSequence s;
        private int offset;
        private int length;

        @Override
        public String answer(InvocationOnMock invocation) {
            s = (CharSequence) invocation.getArguments()[0];
            offset = (Integer) invocation.getArguments()[1];
            length = (Integer) invocation.getArguments()[2];
            return toString();
        }

        public String toString() {
            return s.subSequence(offset, offset + length).toString();
        }
    }
}
