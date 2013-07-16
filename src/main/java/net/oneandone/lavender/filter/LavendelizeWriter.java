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
package net.oneandone.lavender.filter;

import net.oneandone.lavender.processor.Processor;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * A {@link Writer} that redirects the character stream to the {@link Processor}.
 */
public class LavendelizeWriter extends Writer {

    private static final Logger LOG = Logger.getLogger(LavendelizeWriter.class);

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
        } catch (IOException ioe) {
            LOG.fatal("Error in LavendelizeWriter.write(char[],int,int)", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            LOG.fatal("Error in LavendelizeWriter.write(char[],int,int)", re);
            throw re;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        try {
            processor.flush();
        } catch (IOException ioe) {
            LOG.fatal("Error in LavendelizeWriter.flush()", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            LOG.fatal("Error in LavendelizeWriter.flush()", re);
            throw re;
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
        } catch (IOException ioe) {
            LOG.fatal("Error in LavendelizeWriter.close()", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            LOG.fatal("Error in LavendelizeWriter.close()", re);
            throw re;
        }
    }
}
