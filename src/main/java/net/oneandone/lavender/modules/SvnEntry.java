package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Hex;
import org.tmatesoft.svn.core.SVNDirEntry;

import java.util.Arrays;

public class SvnEntry {
    // TODO
    private static final String SEP = "<:>";
    private static final int LEN = SEP.length();

    // path SEP revision SEP size SEP time SEP md5
    public static SvnEntry parse(String str) {
        int idx;
        int prev;
        String relativePath;
        long revision;
        long size;
        long time;
        byte[] md5;

        idx = str.indexOf(SEP);
        relativePath = str.substring(0, idx);
        prev = idx + LEN;
        idx = str.indexOf(SEP, prev);
        revision = Long.parseLong(str.substring(prev, idx));
        prev = idx + LEN;
        idx = str.indexOf(SEP, prev);
        size = Long.parseLong(str.substring(prev, idx));
        prev = idx + LEN;
        idx = str.indexOf(SEP, prev);
        time = Long.parseLong(str.substring(prev, idx));
        prev = idx + LEN;
        md5 = Hex.decode(str.substring(prev).toCharArray());
        return new SvnEntry(relativePath, revision, size, time, md5);
    }

    public static SvnEntry create(SVNDirEntry entry) {
        return new SvnEntry(entry.getRelativePath(), entry.getRevision(), entry.getSize(), entry.getDate().getTime(), null);
    }

    public final String relativePath;
    public final long revision;
    public final long size;
    public final long time;
    public final byte[] md5;

    public SvnEntry(String relativePath, long revision, long size, long time, byte[] md5) {
        if (relativePath.indexOf(SEP) != -1) {
            throw new IllegalArgumentException(relativePath);
        }
        this.relativePath = relativePath;
        this.revision = revision;
        this.size = size;
        this.time = time;
        this.md5 = md5;
    }

    public String toString() {
        return relativePath + SEP + revision + SEP + size + SEP + time + SEP + new String(Hex.encode(md5));
    }

    public int hashCode() {
        return (int) revision;
    }

    public boolean equals(Object obj) {
        SvnEntry entry;

        if (obj instanceof SvnEntry) {
            entry = (SvnEntry) obj;
            return relativePath.equals(entry.relativePath) && revision == entry.revision && size == entry.size && time == entry.time
                    && Arrays.equals(md5, entry.md5);
        }
        return false;
    }
}
