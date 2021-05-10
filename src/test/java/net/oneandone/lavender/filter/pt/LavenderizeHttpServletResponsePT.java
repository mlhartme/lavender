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

import net.oneandone.lavender.filter.LavenderizeHttpServletResponse;
import net.oneandone.lavender.filter.processor.LavenderProcessorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs a performance test.
 */
@Disabled
public class LavenderizeHttpServletResponsePT {

    private static URI requestURI;
    private static String contextPath;
    private static LavenderProcessorFactory processorFactory;
    private static String content;

    @BeforeAll
    public static void init() {
        content = "";
        for (int i = 0; i < 1000; i++) {
            content += "Z";
        }

        requestURI = URI.create("http://localhost:8080/a/b/c.html");
        contextPath = "";
        processorFactory = new LavenderProcessorFactory(null);
    }

    @Test
    public void testOutputStream() throws Exception {

        int numLoops = 10000;
        for (int numThreads = 1; numThreads <= 10; numThreads++) {
            AtomicInteger counter = new AtomicInteger();

            executeRequestWithOutputStream();
            executeRequestWithOutputStream();
            executeRequestWithOutputStream();

            long t0 = System.currentTimeMillis();
            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new OutputStreamRunner(numLoops, counter));
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
            long t1 = System.currentTimeMillis();

            assertEquals(numLoops * numThreads, counter.get());

            long t = t1 - t0;
            long reqPerSec = (1000L * numLoops * numThreads) / t;
            long charsPerSec = (1000L * numLoops * numThreads * content.length()) / t;
            System.out.printf(
                    "LavendelizeHttpServletResponse (OutputStream) multi-threaded performance test: "
                            + "%d threads, %d ms, %,d characters/s, %,d requests/s", numThreads, t, charsPerSec,
                    reqPerSec).println();
        }
    }

    @Test
    public void testWriter() throws Exception {

        int numLoops = 10000;
        for (int numThreads = 1; numThreads <= 10; numThreads++) {
            AtomicInteger counter = new AtomicInteger();

            executeRequestWithWriter();
            executeRequestWithWriter();
            executeRequestWithWriter();

            long t0 = System.currentTimeMillis();
            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new WriterRunner(numLoops, counter));
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
            long t1 = System.currentTimeMillis();

            assertEquals(numLoops * numThreads, counter.get());

            long t = t1 - t0;
            long reqPerSec = (1000L * numLoops * numThreads) / t;
            long charsPerSec = (1000L * numLoops * numThreads * content.length()) / t;
            System.out.printf(
                    "LavendelizeHttpServletResponse (Writer) multi-threaded performance test: "
                            + "%d threads, %d ms, %,d characters/s, %,d requests/s", numThreads, t, charsPerSec,
                    reqPerSec).println();
        }
    }

    private void executeRequestWithOutputStream() throws Exception {
        MockHttpServletResponse wrappedResponse = new MockHttpServletResponse();

        LavenderizeHttpServletResponse response = new LavenderizeHttpServletResponse(wrappedResponse, processorFactory,
                requestURI, null, contextPath, false);
        response.setContentType("text/html; charset=UTF-8");

        ServletOutputStream os = response.getOutputStream();
        os.write(content.getBytes());
        os.close();
        //assertEquals(CONTENT, wrappedResponse.getResult().toString());
    }

    private void executeRequestWithWriter() {
        MockHttpServletResponse wrappedResponse = new MockHttpServletResponse();

        LavenderizeHttpServletResponse response = new LavenderizeHttpServletResponse(wrappedResponse, processorFactory,
                requestURI, null, contextPath, false);
        response.setContentType("text/html; charset=UTF-8");

        PrintWriter writer = response.getWriter();
        writer.write(content);
        writer.close();
        assertEquals(content, wrappedResponse.getResult());
    }

    class OutputStreamRunner implements Runnable {
        private int numLoops;
        private AtomicInteger counter;

        public OutputStreamRunner(int numLoops, AtomicInteger counter) {
            this.numLoops = numLoops;
            this.counter = counter;
        }

        public void run() {
            try {
                for (int i = 0; i < numLoops; i++) {
                    executeRequestWithOutputStream();
                    counter.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    class WriterRunner implements Runnable {
        private int numLoops;
        private AtomicInteger counter;

        public WriterRunner(int numLoops, AtomicInteger counter) {
            this.numLoops = numLoops;
            this.counter = counter;
        }

        public void run() {
            try {
                for (int i = 0; i < numLoops; i++) {
                    executeRequestWithWriter();
                    counter.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

}
