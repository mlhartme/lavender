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
package net.oneandone.lavender.processor;

import net.oneandone.lavender.rewrite.RewriteEngine;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;

/**
 * A {@link Processor} scans content for rewritable URIs, delegates the rewrite to the
 * {@link net.oneandone.lavender.rewrite.RewriteEngine}, and streams the content to the {@link Writer}.
 * @author seelmann
 */
public interface Processor {
    /**
     * Sets the writer where the content is streamed to.
     * @param out
     *            the new writer
     */
    void setWriter(Writer out);

    /**
     * Sets the rewrite engine, used to rewrite URIs.
     * @param rewriteEngine
     *            the new rewrite engine
     * @param baseURI
     *            the base URI, used to resolve URI references
     * @param contextPath
     *            the context path, used to resolve URI references
     */
    void setRewriteEngine(RewriteEngine rewriteEngine, URI baseURI, String contextPath);

    /**
     * Processes a character stream. The implementation must process all characters available in the
     * {@link CharSequence} because the {@link CharSequence} is reused.
     * @param chars
     *            the character sequence to process
     * @param offset
     *            offset from which to start processing characters
     * @param length
     *            number of characters to process
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    void process(CharSequence chars, int offset, int length) throws IOException;

    /**
     * Process all outstanding characters that may be bufferd.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    void flush() throws IOException;

    void close() throws IOException;
}
