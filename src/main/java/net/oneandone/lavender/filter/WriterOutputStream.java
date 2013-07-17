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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * A {@link ServletOutputStream} that pipes output to writer, using the specified decoder/encoding.
 */
public class WriterOutputStream extends ServletOutputStream {
    public static WriterOutputStream create(Writer wrapped, String encoding) {
        CharsetDecoder decoder;

        decoder = Charset.forName(encoding).newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new WriterOutputStream(wrapped, decoder, 1024);

    }

    private static final Logger LOG = LoggerFactory.getLogger(WriterOutputStream.class);

    /** The wrapped writer. */
    private final Writer wrapped;

    /** The decoder, used to convert decode written bytes to characters. */
    private final CharsetDecoder decoder;

    /** The byte buffer, used by the decoder. */
    private final ByteBuffer bb;

    /** The character buffer, used by the decoder. */
    private final CharBuffer cb;

    /** A byte array, used to process a single byte. */
    private final byte[] single;

    private boolean closed;

    public WriterOutputStream(Writer wrapped, CharsetDecoder decoder, int bufferSize) {
        this.wrapped = wrapped;
        this.decoder = decoder;
        this.bb = ByteBuffer.allocate(bufferSize);
        this.cb = CharBuffer.allocate(bufferSize);
        this.single = new byte[1];
        this.closed = false;
    }

    /**
     * {@inheritDoc}
     */
    public void write(int b) throws IOException {
        try {
            single[0] = (byte) b;
            doWrite(single, 0, 1);
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in WriterOutputStream.write(int)", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b) throws IOException {
        try {
            doWrite(b, 0, b.length);
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in WriterOutputStream.write(byte[])", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            doWrite(b, off, len);
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in WriterOutputStream.write(byte[],int,int)", e);
            throw e;
        }
    }

    private void doWrite(byte[] b, int off, int len) throws IOException {
        decoder.reset();

        int o = off;
        int l = 0;

        do {
            o += l;
            l = len - o + off;
            l = Math.min(l, bb.remaining());

            bb.put(b, o, l);
            bb.flip();

            CoderResult result = decoder.decode(bb, cb, false);
            if (result.isError()) {
                result.throwException();
            }

            cb.flip();
            wrapped.append(cb, cb.position(), cb.length());
            cb.clear();
            bb.compact();
        } while (o + l < off + len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        try {
            doFlush();
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in WriterOutputStream.flush()", e);
            throw e;
        }
    }

    private void doFlush() throws IOException {
        wrapped.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            // already closed
            return;
        }
        closed = true;
        try {
            doClose();
        } catch (IOException ioe) {
            LOG.error("Error in WriterOutputStream.close()", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            LOG.error("Error in WriterOutputStream.close()", re);
            throw re;
        }
    }

    private void doClose() throws IOException {
        CoderResult result;
        CoderResult flushResult;

        bb.flip();
        result = decoder.decode(bb, cb, true);
        if (result.isError()) {
            result.throwException();
        }
        flushResult = decoder.flush(cb);
        if (flushResult.isError()) {
            flushResult.throwException();
        }
        cb.flip();
        wrapped.append(cb, cb.position(), cb.length());
        cb.clear();
        doFlush();
        wrapped.close();
    }
}
