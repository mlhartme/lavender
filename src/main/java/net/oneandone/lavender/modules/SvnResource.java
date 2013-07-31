package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.index.Resource;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.net.URI;

public class SvnResource extends Resource {
    private final SvnModule module;
    private final long revision;

    public SvnResource(SvnModule module, long revision, URI origin, String path, long length, long lastModified, String folder, Node dataNode, byte[] dataBytes, byte[] lazyMd5) {
        super(origin, path, length, lastModified, folder, dataNode, dataBytes, lazyMd5);
        this.module = module;
        this.revision = revision;
    }

    @Override
    public byte[] getMd5() throws IOException {
        if (lazyMd5 == null) {
            super.getMd5();
            module.index().add(new Label(getPath(), Long.toString(revision), lazyMd5));
        }
        return lazyMd5;
    }
}
