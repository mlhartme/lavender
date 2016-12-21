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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * A {@link Writer} that redirects the character stream to the {@link Processor}.
 */
public class LavendelizeWriter extends Writer {

    private static final Logger LOG = LoggerFactory.getLogger(LavendelizeWriter.class);

    /** The content processor. */
    private final Processor processor;

    private boolean closed;

    /**
     * Instantiates a new lavendelize writer.
     */
    public LavendelizeWriter(Processor processor) {
        this.processor = processor;
        this.closed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        try {
            processor.process(CharBuffer.wrap(cbuf), off, len);
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in LavendelizeWriter.write(char[],int,int)", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        try {
            processor.flush();
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in LavendelizeWriter.flush()", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            processor.close();
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in LavendelizeWriter.close()", e);
            throw e;
        }
    }
}
