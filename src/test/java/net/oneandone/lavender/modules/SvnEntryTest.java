package net.oneandone.lavender.modules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SvnEntryTest {
    @Test
    public void encode() {
        String str;

        check("", "");
        check("123", "123");
        check("%25", "%");
        check("a%20b", "a b");
        check("%20%25", " %");
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            str = Character.toString((char) i);
            check(str);
            check("123" + str);
            check(str + "xyz");
        }
    }

    private void check(String decoded) {
        check(SvnEntry.encode(decoded), decoded);
    }

    private void check(String encoded, String decoded) {
        assertEquals(encoded, SvnEntry.encode(decoded));
        assertEquals(decoded, SvnEntry.decode(encoded));
    }

    @Test
    public void parse() {
        SvnEntry entry;
        SvnEntry parsed;

        entry = new SvnEntry("my path", 1, 2, 3, new byte[] { 4, 5, 6});
        parsed = SvnEntry.parse(entry.toString());
        assertEquals(entry, parsed);
    }
}
