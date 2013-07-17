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
package net.oneandone.lavender.rewrite;

import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

/**
 * RewriteEngine that is able to load and push resources during rewrite.
 */
public class RewriteEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RewriteEngine.class);

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
