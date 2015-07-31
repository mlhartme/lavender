package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Hex;

import java.util.Arrays;

public class SvnEntry {
    // TODO
    private static final char SEP = ' ';
    private static final char ESCAPE = '%';
    private static final char LF = '\n';
    private static final int LEN = 1;

    // path SEP revision SEP size SEP time SEP md5
    public static SvnEntry parse(String str) {
        int idx;
        int prev;
        String publicPath;
        String accessPath;
        long revision;
        long size;
        long time;
        byte[] md5;

        idx = str.indexOf(SEP);
        publicPath = decode(str.substring(0, idx));
        prev = idx + LEN;
        idx = str.indexOf(SEP, prev);
        accessPath = decode(str.substring(prev, idx));
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
        if (prev == str.length()) {
            md5 = null;
        } else {
            md5 = Hex.decode(str.substring(prev).toCharArray());
        }
        return new SvnEntry(publicPath, accessPath, revision, size, time, md5);
    }

    public final String publicPath;
    public final String accessPath;
    public final long revision;
    public final long size;
    public final long time;
    public byte[] md5;

    public SvnEntry(String publicPath, String accessPath, long revision, long size, long time, byte[] md5) {
        this.publicPath = publicPath;
        this.accessPath = accessPath;
        this.revision = revision;
        this.size = size;
        this.time = time;
        this.md5 = md5;
    }

    public String toString() {
        return encode(publicPath) + SEP + encode(accessPath)
                + SEP + revision + SEP + size + SEP + time + SEP + (md5 == null ? "" : new String(Hex.encode(md5)));
    }

    public int hashCode() {
        return (int) revision;
    }

    public boolean equals(Object obj) {
        SvnEntry entry;

        if (obj instanceof SvnEntry) {
            entry = (SvnEntry) obj;
            return publicPath.equals(entry.publicPath) && accessPath.equals(entry.accessPath)
                    && revision == entry.revision && size == entry.size && time == entry.time
                    && Arrays.equals(md5, entry.md5);
        }
        return false;
    }

    public static String decode(String str) {
        int prev;
        int idx;
        StringBuilder decoded;
        char c;

        idx = str.indexOf(ESCAPE);
        if (idx == -1) {
            return str;
        }
        prev = 0;
        decoded = new StringBuilder(str.length());
        do {
            decoded.append(str.substring(prev, idx));
            decoded.append(decoded(str.charAt(idx + 1), str.charAt(idx + 2)));
            prev = idx + 3;
            idx = str.indexOf(ESCAPE, prev);
        } while (idx != -1);
        decoded.append(str.substring(prev));
        return decoded.toString();

    }

    private static char decoded(char c2, char c1) {
        return (char) (Hex.decode(c2) << 4 | Hex.decode(c1));
    }

    public static String encode(String str) {
        StringBuilder encoded;
        int len;
        char c;

        if (str.indexOf(SEP) == -1 && str.indexOf(ESCAPE) == -1 && str.indexOf(LF) == -1) {
            return str;
        }
        len = str.length();
        encoded = new StringBuilder(len + 4);
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            switch (c) {
                case SEP:
                case ESCAPE:
                case LF:
                    encoded.append(ESCAPE);
                    encoded.append(Hex.encode((0x00F0 & c) >>> 4));
                    encoded.append(Hex.encode((0x000F & c) >>> 0));
                    break;
                default:
                    encoded.append(c);
            }
        }
        return encoded.toString();
    }
}
