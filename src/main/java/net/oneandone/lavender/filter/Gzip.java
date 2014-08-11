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
package net.oneandone.lavender.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * gzip handling
 */
public final class Gzip {
    public static boolean canGzip(HttpServletRequest request) {
        String accepted;

        accepted = request.getHeader("Accept-Encoding");
        return accepted != null && contains(accepted, "gzip");
    }

    // see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
    public static boolean contains(String list, String keyword) {
        int idx;
        int colon;
        String quality;

        idx = list.indexOf(keyword);
        if (idx == -1) {
            return false;
        }
        idx += keyword.length();
        colon = list.indexOf(",", idx);
        if (colon == -1) {
            colon = list.length();
        }
        quality = list.substring(idx, colon);
        idx = quality.indexOf('=');
        if (idx == -1) {
            return true;
        }
        return !"0".equals(quality.substring(idx + 1).trim());
    }

    private Gzip() {
    }
}
