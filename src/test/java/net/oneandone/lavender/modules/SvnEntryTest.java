package net.oneandone.lavender.modules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SvnEntryTest {
    @Test
    public void parse() {
        SvnEntry entry;
        SvnEntry parsed;

        entry = new SvnEntry("myPath", 1, 2, 3, new byte[] { 4, 5, 6});
        parsed = SvnEntry.parse(entry.toString());
        assertEquals(entry, parsed);
    }
}
