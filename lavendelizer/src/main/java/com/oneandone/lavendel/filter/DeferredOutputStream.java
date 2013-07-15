package com.oneandone.lavendel.filter;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** OutputStream that defers creation of the target output stream until the target is actually used. */
public abstract class DeferredOutputStream extends ServletOutputStream {
    private OutputStream target;

    public DeferredOutputStream() {
        target = null;
    }

    protected abstract OutputStream createTarget() throws IOException;

    private OutputStream target() throws IOException {
        if (target == null) {
            target = createTarget();
        }
        return target;
    }

    @Override
    public void write(int b) throws IOException {
        target().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        target().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        target().flush();
    }

    @Override
    public void close() throws IOException {
        target().close();
    }
}
