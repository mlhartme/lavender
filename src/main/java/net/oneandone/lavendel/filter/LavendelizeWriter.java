package net.oneandone.lavendel.filter;

import net.oneandone.lavendel.processor.Processor;
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
