package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.io.Buffer;

import java.io.IOException;
import java.io.InputStream;

public class SvnResource extends Resource {
    private final SvnModule module;
    private final long revision;
    private final String path;
    private final int length;
    private final long lastModified;

    public SvnResource(SvnModule module, long revision, String path, int length, long lastModified, Node dataNode, byte[] lazyMd5) {
        this.module = module;
        this.revision = revision;
        this.path = path;
        this.length = length;
        this.lastModified = lastModified;

        this.dataNode = dataNode;
        this.dataBytes = null;

        this.lazyMd5 = lazyMd5;
    }

    @Override
    public byte[] getMd5() throws IOException {
        if (lazyMd5 == null) {
            lazyMd5 = md5(getData());
            module.index().add(new Label(getPath(), Long.toString(revision), lazyMd5));
        }
        return lazyMd5;
    }

    // dataNode xor dataBytes is null
    private Node dataNode;
    private byte[] dataBytes;

    protected byte[] lazyMd5;

    public String getPath() {
        return path;
    }

    public long getSize() {
        return length;
    }

    public long getLastModified() throws IOException {
        return lastModified;
    }

    public String getOrigin() {
        return module.uri() + "/" + path;
    }

    public byte[] getData() throws IOException {
        if (dataBytes == null) {
            dataBytes = new byte[length];
            try (InputStream src = dataNode.createInputStream()) {
                if (length != Buffer.fill(src, dataBytes)) {
                    throw new IllegalStateException();
                }
                if (src.read() != -1) {
                    throw new IllegalStateException();
                }
            }
            dataNode = null;
        }
        return dataBytes;
    }
}
