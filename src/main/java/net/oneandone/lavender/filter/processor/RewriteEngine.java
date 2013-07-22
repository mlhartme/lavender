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
package net.oneandone.lavender.filter.processor;

import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * RewriteEngine that is able to load and push resources during rewrite.
 */
public class RewriteEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RewriteEngine.class);

    protected final Index index;

    /** The nodes used for HTTP */
    protected final Map<String, URI> httpNodes = new HashMap<>();

    /** The nodes used for HTTPS */
    protected final Map<String, URI> httpsNodes = new HashMap<>();

    /** The consistent hash function. */
    protected final ConsistentHash consistentHash;

    public RewriteEngine(Index index, URL nodesUrl) throws IOException {
        this.index = index;
        InputStream in;

        in = nodesUrl.openStream();
        if (in == null) {
            throw new IllegalStateException("nodes file not found: " + nodesUrl);
        }
        readNodes(in);
        in.close();
        this.consistentHash = new ConsistentHash(200, httpNodes.keySet().toArray(new String[0]));
    }


    private void readNodes(InputStream raw) throws IOException {
        BufferedReader in;
        String line;

        in = new BufferedReader(new InputStreamReader(raw, Index.ENCODING));
        while (true) {
            line = in.readLine();
            if (line == null) {
                return;
            }
            line = line.trim();
            if (!line.isEmpty()) {
                if (!line.endsWith("/")) {
                    line = line + "/";
                }
                URI uri = URI.create(line);
                if ("http".equals(uri.getScheme())) {
                    httpNodes.put(uri.getHost(), uri);
                } else if ("https".equals(uri.getScheme())) {
                    httpsNodes.put(uri.getHost(), uri);
                } else {
                    throw new IllegalArgumentException("Node " + line + " has unsupported schema, only http and https are supported.");
                }
            }
        }
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

        return calculateURL(label, baseURI);
    }

    public URI calculateURL(Label label, URI baseURI) {
        if (label.getLavendelizedPath() == null) {
            throw new IllegalStateException();
        }
        byte[] md5 = label.md5();
        String node = consistentHash.getNodeForHash(md5);
        String lavendelizedPath = label.getLavendelizedPath();
        URI nodeURI = baseURI.getScheme().equals("https") ? httpsNodes.get(node) : httpNodes.get(node);
        String path = nodeURI.getPath() + lavendelizedPath;
        int port = nodeURI.getPort();
        try {
            return new URI(nodeURI.getScheme(), null, node, port, path, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
