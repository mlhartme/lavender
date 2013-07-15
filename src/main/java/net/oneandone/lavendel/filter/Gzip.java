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
package net.oneandone.lavendel.filter;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * gzip handling
 */
public final class Gzip {
    private static final Logger LOG = Logger.getLogger(Gzip.class);

    public static boolean canGzip(HttpServletRequest request) {
        String accepted;
        String userAgent;

        accepted = request.getHeader("Accept-Encoding");
        if (accepted == null) {
            return false;
        }
        if (!contains(accepted, "gzip")) {
            return false;
        }
        userAgent = request.getHeader("User-Agent");
        if (userAgent == null || !whiteListed(userAgent)) {
            LOG.info("user-agent not white-listed for gzip: " + userAgent);
            return false;
        }
        return true;
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

    // see http://msdn.microsoft.com/en-us/library/ms537503(VS.85).aspx
    private static final Pattern MSIE = Pattern.compile("Mozilla/4.0 \\(compatible; MSIE (\\d+)\\..*");

    private static final Pattern MOZILLA = Pattern.compile("Mozilla/(\\d+)\\..*");

    public static boolean whiteListed(String str) {
        if (atLeast(str, MSIE, 7)) {
            return true;
        }
        if (atLeast(str, MOZILLA, 5)) {
            return true;
        }
        if (str.contains("HttpClient")) {  // for testing
            return true;
        }
        return false;
    }

    private static boolean atLeast(String str, Pattern pattern, int num) {
        String version;
        Matcher matcher;

        matcher = pattern.matcher(str);
        if (matcher.matches()) {
            version = matcher.group(1);
            return Integer.parseInt(version) >= num;
        }
        return false;
    }

    private Gzip() {
    }
}
