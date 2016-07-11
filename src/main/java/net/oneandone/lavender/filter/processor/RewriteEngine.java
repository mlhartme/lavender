package net.oneandone.lavender.filter.processor;

import java.net.URI;

public interface RewriteEngine {
    String rewrite(String reference, URI baseURI, String contextPath);
}
