package net.oneandone.lavender.modules;

public class SvnFile {
    public final String path;
    public final long revision;

    public SvnFile(String path, long revision) {
        this.path = path;
        this.revision = revision;
    }
}
