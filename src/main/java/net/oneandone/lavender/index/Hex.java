/*
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
package net.oneandone.lavender.index;

public final class Hex {
    public static byte[] decodeString(String str) {
        int max;
        byte[] result;
        int f;

        max = str.length();
        if (max % 2 != 0) {
            throw new IllegalArgumentException();
        }
        result = new byte[max / 2];
        for (int i = 0, j = 0; j < max; i++) {
            f = decode(str.charAt(j)) << 4;
            j++;
            f = f | decode(str.charAt(j));
            j++;
            result[i] = (byte) (f & 0xFF);
        }
        return result;
    }

    public static int decode(char ch) {
        return ch >= 'a' ? ch - 'a' + 10 : (ch >= 'A' ? ch - 'A' + 10 : ch - '0');
    }

    public static char[] encode(byte[] data) {
        int max;
        char[] result;

        max = data.length;
        result = new char[max << 1];
        for (int i = 0, j = 0; i < max; i++) {
            result[j++] = encode((0xF0 & data[i]) >>> 4);
            result[j++] = encode(0x0F & data[i]);
        }
        return result;
    }

    public static String encodeString(byte[] data) {
        return new String(encode(data));
    }

    public static char encode(int b) {
        return (char) (b >= 10 ? 'a' - 10 + b : '0' + b);
    }

    private Hex() {
    }
}
