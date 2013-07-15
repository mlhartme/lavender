package com.oneandone.lavendel.filter;

import java.io.IOException;
import java.io.Writer;

/** Writer that defers creation of the target writer until the target is actually used. */
public abstract class DeferredWriter extends Writer {
    private Writer target;

    public DeferredWriter() {
        this.target = null;
    }

    protected abstract Writer createTarget() throws IOException;

    private Writer target() throws IOException {
        if (target == null) {
            target = createTarget();
        }
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        target().write(cbuf, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        target().flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        target().close();
    }
}
