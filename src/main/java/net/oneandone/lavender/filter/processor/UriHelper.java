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

}
