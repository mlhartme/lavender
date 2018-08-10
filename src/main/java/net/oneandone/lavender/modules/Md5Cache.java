package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Util;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Md5Cache {
    public static Md5Cache loadOrCreate(World world, String module) throws IOException {
        FileNode file;
        String[] parts;
        Md5Cache result;

        file = world.getTemp().join(module + ".cache");
        result = new Md5Cache(file);
        if (file.exists()) {
            for (String line : file.readLines()) {
                parts = line.split(",");
                if (parts.length != 3) {
                    throw new IOException("invalid cache line");
                }
                result.add(parts[0], parts[1], Hex.decode(parts[2].toCharArray()));
            }
        }
        return result;
    }

    private static class Entry {
        public final String path;
        public final String contentId;
        public final byte[] md5;

        private Entry(String path, String contentId, byte[] md5) {
            this.path = path;
            this.contentId = contentId;
            this.md5 = md5;
        }
    }

    private final FileNode file;
    private final List<Entry> entries;

    public Md5Cache(FileNode file) {
        this.file = file;
        this.entries = new ArrayList<>();
    }

    public void add(String path, String contentId, byte[] md5) {
        int idx;

        idx = lookup(path);
        if (idx != -1) {
            entries.remove(idx);
        }
        entries.add(new Entry(path, contentId, md5));
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
                writer.write(entry.path);
                writer.write(',');
                writer.write(entry.contentId);
                writer.write(',');
                writer.write(Hex.encode(entry.md5));
                writer.write('\n');
            }
        }
        tmp.move(file, true);
    }
}
