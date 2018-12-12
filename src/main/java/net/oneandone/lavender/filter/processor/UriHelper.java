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
package net.oneandone.lavender.filter.processor;

import java.net.URI;

public class UriHelper {

    public static String removeLeadingTrailingQuotes(String uri){
        int len = uri.length();
        if (len > 2) {
            if ((uri.startsWith("\"") && uri.endsWith("\"")) || (uri.startsWith("'") && uri.endsWith("'"))) {
                // this is a broken uri, but we fix it here because this error to way too common
                uri = uri.substring(1, len - 1);
            }
        }
        return uri;
    }

    public static String resolvePathWithoutContext(URI reference, URI baseURI, String contextPath) {
        URI uri = baseURI.resolve(reference);
        String resolved = uri.getPath();
        if (resolved == null) {
            return null;
        }
        if (resolved.startsWith(contextPath)) {
            resolved = resolved.substring(contextPath.length());
        }
        return resolved;
    }

    private UriHelper() {
    }
}
