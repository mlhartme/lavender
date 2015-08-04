/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        entry = new SvnEntry("my path", "origPath", 1, 2, 3, new byte[] { 4, 5, 6});
        parsed = SvnEntry.parse(entry.toString());
        assertEquals(entry, parsed);
    }
}
