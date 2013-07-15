/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.oneandone.lavendel.index;

public final class Hex {
    public static byte[] decode(char[] data) {
        int max;
        byte[] result;
        int f;

        max = data.length;
        if (max % 2 != 0) {
            throw new IllegalArgumentException();
        }
        result = new byte[max/2];
        for (int i = 0, j = 0; j < max; i++) {
            f = decode(data[j]) << 4;
            j++;
            f = f | decode(data[j]);
            j++;
            result[i] = (byte) (f & 0xFF);
        }
        return result;
    }

    private static int decode(char ch) {
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

    private static char encode(int b) {
        return (char) (b >= 10 ? 'a' - 10 + b : '0' + b);
    }

    private Hex() {
    }
}
