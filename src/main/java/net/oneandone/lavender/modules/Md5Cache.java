package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Util;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Md5Cache implements AutoCloseable {
    public static Md5Cache loadOrCreate(FileNode file) throws IOException {
        Md5Cache result;

        result = new Md5Cache(file);
        if (file.exists()) {
            for (String line : file.readLines()) {
                result.add(Entry.parse(line));
            }
        }
        return result;
    }

    private static class Entry {
        public static Entry parse(String str) {
            int idx;
            int prev;
            String path;
            String contentId;
            byte[] md5;

            idx = str.indexOf(SvnEntry.SEP);
            path = SvnEntry.decode(str.substring(0, idx));
            prev = idx + SvnEntry.LEN;
            idx = str.indexOf(SvnEntry.SEP, prev);
            contentId = SvnEntry.decode(str.substring(prev, idx));
            prev = idx + SvnEntry.LEN;
            md5 = Hex.decode(str.substring(prev).toCharArray());
            return new Entry(path, contentId, md5);
        }

        public final String path;
        public final String contentId;
        public final byte[] md5;

        private Entry(String path, String contentId, byte[] md5) {
            this.path = path;
            this.contentId = contentId;
            this.md5 = md5;
        }

        public String toString() {
            return SvnEntry.encode(path) + SvnEntry.SEP + SvnEntry.encode(contentId) + SvnEntry.SEP + Hex.encodeString(md5);
        }
    }

    private final FileNode file;
    private final List<Entry> entries;
    private boolean modified;

    public Md5Cache(FileNode file) {
        this.file = file;
        this.entries = new ArrayList<>();
        this.modified = false;
    }

    public void add(String path, String contentId, byte[] md5) {
        add(new Entry(path, contentId, md5));
    }

    public void add(Entry entry) {
        int idx;

        idx = lookup(entry.path);
        if (idx != -1) {
            entries.remove(idx);
        }
        entries.add(entry);
        modified = true;
    }

    private int lookup(String path) {
        for (int i = 0, max = entries.size(); i < max; i++) {
            if (entries.get(i).path.equals(path)) {
                return i;
            }
        }
        return -1;
    }

    public byte[] lookup(String path, String contentId) {
        for (Entry entry : entries) {
            if (entry.path.equals(path) && entry.contentId.equals(contentId)) {
                return entry.md5;
            }
        }
        return null;
    }

    public void save() throws IOException {
        FileNode parent;
        FileNode tmp;

        file.getParent().mkdirsOpt();
        // first write to a temp file, then move it (which is atomic) because
        // * no corruption by crashed/killed processes
        // * works for multiple users as long as the cache directory has the proper permissions
        parent = file.getParent();
        tmp = Util.newTmpFile(parent);
        try (Writer writer = tmp.newWriter()) {
            for (Entry entry : entries) {
                writer.write(entry.toString());
                writer.write(SvnEntry.LF);
            }
        }
        tmp.move(file, true);
        modified = false;
    }

    @Override
    public void close() throws IOException {
        if (modified) {
            save();
        }
    }
}
