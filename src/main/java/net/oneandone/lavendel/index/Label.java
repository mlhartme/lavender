package net.oneandone.lavendel.index;

/** A line in the index file. Immutable. */
public class Label {
    private final String originalPath;
    private final String lavendelizedPath;
    private final byte[] md5;

    public Label(String originalPath, String lavendelizedPath, byte[] md5) {
        this.originalPath = originalPath;
        this.lavendelizedPath = lavendelizedPath;
        this.md5 = md5;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getLavendelizedPath() {
        return lavendelizedPath;
    }

    public byte[] md5() {
        return md5;
    }

    public String toString() {
        return String.format("Label [originalPath=%s, lavendelizedPath=%s, md5=%s]", originalPath, lavendelizedPath, md5);
    }
}
