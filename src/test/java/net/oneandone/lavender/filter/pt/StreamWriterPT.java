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
package net.oneandone.lavender.filter.pt;

import net.oneandone.sushi.io.MultiOutputStream;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@Ignore
public class StreamWriterPT {

    @Test
    public void testOutputStreamWriter() throws IOException {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);
            OutputStreamWriter osw = new OutputStreamWriter(baos);

            for (int i = 0; i < 1000000; i++) {
                osw.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long charsPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("OutputStreamWriter: t=" + t + ", " + charsPerSec + "/s");
    }

    @Test
    public void testOutputStreamWriterWithBufferedStream() throws IOException {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);
            BufferedOutputStream bos = new BufferedOutputStream(baos);
            OutputStreamWriter osw = new OutputStreamWriter(bos);

            for (int i = 0; i < 1000000; i++) {
                osw.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long charsPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("OutputStreamWriterWithBufferedStream: t=" + t + ", " + charsPerSec + "/s");
    }

    @Test
    public void testOutputStreamWriterWithBufferedWriter() throws IOException {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);
            OutputStreamWriter osw = new OutputStreamWriter(baos);
            BufferedWriter bw = new BufferedWriter(osw);

            for (int i = 0; i < 1000000; i++) {
                bw.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long charsPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("OutputStreamWriterWithBufferedWriter: t=" + t + ", " + charsPerSec + "/s");
    }

    @Test
    public void testOutputStreamWriterFullBuffered() throws IOException {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);
            BufferedOutputStream bos = new BufferedOutputStream(baos);
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            BufferedWriter bw = new BufferedWriter(osw);

            for (int i = 0; i < 1000000; i++) {
                bw.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long charsPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("OutputStreamWriterFullBuffered: t=" + t + ", " + charsPerSec + "/s");
    }

    @Test
    public void testOutputStream() {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);

            for (int i = 0; i < 1000000; i++) {
                baos.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long bytesPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("OutputStream: t=" + t + ", " + bytesPerSec + "/s");
    }

    @Test
    public void testOutputStreamWithBuffer() throws IOException {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);
            BufferedOutputStream bos = new BufferedOutputStream(baos);

            for (int i = 0; i < 1000000; i++) {
                bos.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long bytesPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("OutputStreamWithBuffer: t=" + t + ", " + bytesPerSec + "/s");
    }

    @Test
    public void testNullOutputStream() throws IOException {

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            OutputStream nos = MultiOutputStream.createNullStream();
            for (int i = 0; i < 1000000; i++) {
                nos.write('a');
            }
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        long bytesPerSec = 1000L * 1000000 * 100 / t;
        System.out.println("NullOutputStream: t=" + t + ", " + bytesPerSec + "/s");
    }

}
