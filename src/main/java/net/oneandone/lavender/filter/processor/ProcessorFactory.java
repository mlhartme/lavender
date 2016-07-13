package net.oneandone.lavender.filter.processor;

import java.net.URI;

public interface ProcessorFactory {
    Processor createProcessor(String baseContentType, URI baseURI, String contextPath);
}
