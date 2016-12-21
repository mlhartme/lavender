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
package net.oneandone.lavender.filter.processor;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;

/**
 * Base implementation of {@link Processor}.
 */
public abstract class AbstractProcessor implements Processor {

    /** The logger. */
    private final Logger log;

    /** The writer where the content is streamed to. */
    protected Writer out;

    /** The rewrite engine. */
    protected RewriteEngine rewriteEngine;

    /** The base URI used, used to resolve URI references. */
    protected URI baseURI;

    /** The context path used, used to resolve URI references. Always starts and ends with a "/" */
    protected String contextPath;

    /** Buffer where a potential URI string is stored before it is rewritten. */
    protected StringBuilder uriBuffer = new StringBuilder(128);

    /**
     * Subclass constructor.
     * @param logger
     *            the logger
     */
    protected AbstractProcessor(Logger logger) {
        log = logger;
    }

    /**
     * {@inheritDoc}
     */
    public void setWriter(Writer writer) {
        this.out = writer;
    }

    /**
     * {@inheritDoc}
     */
    public void setRewriteEngine(RewriteEngine theRewriteEngine, URI theBaseURI, String theContextPath) {
        if (!theContextPath.startsWith("/")) {
            throw new IllegalArgumentException(theContextPath);
        }
        if (!theContextPath.endsWith("/")) {
            throw new IllegalArgumentException(theContextPath);
        }
        this.rewriteEngine = theRewriteEngine;
        this.baseURI = theBaseURI;
        this.contextPath = theContextPath;
    }

    /**
     * {@inheritDoc}
     */
    public void process(CharSequence chars, int offset, int length) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Processing chars.length=" + chars.length() + ", offset=" + offset + ", length=" + length + "\n"
                    + chars.subSequence(offset, offset + length).toString());
        } else if (log.isDebugEnabled()) {
            log.debug("Processing chars.length=" + chars.length() + ", offset=" + offset + ", length=" + length);
        }

        for (int i = offset; i < offset + length; i++) {
            char c = chars.charAt(i);
            process(c);
        }
    }

    /**
     * Processes a single character.
     * @param c
     *            the character to process
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected abstract void process(char c) throws IOException;

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        if (uriBuffer.length() > 0) {
            out.write(uriBuffer.toString());
            uriBuffer.setLength(0);
        }
        out.flush();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        flush();
        out.close();
    }
}
