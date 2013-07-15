package net.oneandone.lavendel.rewrite;

import net.oneandone.lavendel.index.Index;
import net.oneandone.lavendel.index.Label;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses a consistent hash function to calculate the host name for a particular resource.
 * @author seelmann
 */
public class UrlCalculator {
    /** The nodes used for HTTP */
    protected final Map<String, URI> httpNodes = new HashMap<String, URI>();

    /** The nodes used for HTTPS */
    protected final Map<String, URI> httpsNodes = new HashMap<String, URI>();

    /** The consistent hash function. */
    protected final ConsistentHash consistentHash;

    public UrlCalculator(URL nodesUrl) throws IOException {
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
}
