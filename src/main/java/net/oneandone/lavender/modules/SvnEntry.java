package net.oneandone.lavender.modules;

import org.tmatesoft.svn.core.SVNDirEntry;

public class SvnEntry {
    public static SvnEntry create(SVNDirEntry entry) {
        return new SvnEntry(entry.getRelativePath(), entry.getRevision(), entry.getSize(), entry.getDate().getTime());
    }

    public final String relativePath;
    public final long revision;
    public final long size;
    public final long time;

    public SvnEntry(String relativePath, long revision, long size, long time) {
        this.relativePath = relativePath;
        this.revision = revision;
        this.size = size;
        this.time = time;
    }
}
