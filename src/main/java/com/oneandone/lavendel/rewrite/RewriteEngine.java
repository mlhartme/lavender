package com.oneandone.lavendel.rewrite;

import com.oneandone.lavendel.index.Index;
import com.oneandone.lavendel.index.Label;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

/**
 * RewriteEngine that is able to load and push resources during rewrite.
 * @author seelmann
 */
public class RewriteEngine {
    private static final Logger LOG = Logger.getLogger(RewriteEngine.class);

    protected final Index index;
    protected final UrlCalculator urlCalculator;

    public RewriteEngine(Index index, UrlCalculator urlCalculator) {
        this.index = index;
        this.urlCalculator = urlCalculator;
    }

    public String rewrite(String uri, URI baseURI, String contextPath) {
        URI reference;
        String result;

        try {
            reference = new URI(uri);
        } catch (URISyntaxException e) {
            LOG.warn("cannot rewrite invalid URI '" + uri + "'");
            return uri;
        }
        result = rewrite(reference, baseURI, contextPath).toASCIIString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("rewrite ok: '" + uri + "' -> '" + result + "'");
        }
        return result;
    }


    public URI rewrite(URI reference, URI baseURI, String contextPath) {
        Label label;

        label = lookup(reference, baseURI, contextPath);
        if (label == null) {
            if (LOG.isDebugEnabled()) {
                String message = "No resource found in index for reference={0}, baseURI={1}, contextPath={2}";
                String formatted = MessageFormat.format(message, reference, baseURI, contextPath);
                LOG.debug(formatted);
            }
            return reference;
        }

        return urlCalculator.calculateURL(label, baseURI);
    }

    Label lookup(URI reference, URI baseURI, String contextPath) {
        String resolved;
        Label label;

        resolved = resolve(reference, baseURI, contextPath);
        label = resolved == null ? null : index.lookup(resolved);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Lookup index for reference " + reference + "(resolved=" + resolved + "): " + label);
        }
        return label;
    }

    String resolve(URI reference, URI baseURI, String contextPath) {
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
