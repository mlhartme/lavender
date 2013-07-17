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

import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.processor.ProcessorFactory;
import net.oneandone.lavender.publisher.Distributor;
import net.oneandone.lavender.publisher.Source;
import net.oneandone.lavender.rewrite.RewriteEngine;
import net.oneandone.lavender.rewrite.UrlCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A servlet filter that <em>lavendelizes</em> the response content.
 * @author seelmann
 */
public class Lavender implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(Lavender.class);

    /** The filter configuration. */
    protected FilterConfig filterConfig;

    protected ProcessorFactory processorFactory;

    public static final String LAVENDEL_IDX = "/WEB-INF/lavender.idx";
    public static final String LAVENDEL_NODES = "/WEB-INF/lavender.nodes";

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig config) throws ServletException {
        URL src;
        Index index;
        UrlCalculator urlCalculator;
        RewriteEngine rewriteEngine;

        this.filterConfig = config;
        src = resourceOpt(LAVENDEL_IDX);
        if (src == null) {
            processorFactory = null;
            LOG.info("Lavendel devel filter");
        } else {
            try {
                index = new Index(src);
                urlCalculator = new UrlCalculator(resourceOpt(LAVENDEL_NODES));
                rewriteEngine = new RewriteEngine(index, urlCalculator);
                processorFactory = new ProcessorFactory(rewriteEngine);
                LOG.info("Lavender prod filter");
            } catch (IOException ie) {
                LOG.error("Error in Lavendelizer.init()", ie);
                throw new ServletException("io error", ie);
            } catch (ServletException | RuntimeException se) {
                LOG.error("Error in Lavendelizer.init()", se);
                throw se;
            }
        }
    }

    private URL resourceOpt(String path) throws ServletException {
        try {
            return filterConfig.getServletContext().getResource(path);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (processorFactory == null) {
            doDevelFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else {
            doProdFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }
    }

    public void doDevelFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LOG.info("pass-through: " + request);
        chain.doFilter(request, response);
/* TODO
        List<Source> extractors;
        Index index;
        long changed;

        extractors = Source.fromWar(log, inputWar, svnUsername, svnPassword);
        changed = extract(extractors);
        for (Map.Entry<String, Distributor> entry : storages.entrySet()) {
            index = entry.getValue().close();
            //  TODO
            if (!entry.getKey().contains("flash") && index != null) {
                for (Label label : index) {
                    outputIndex.add(label);
                }
            }
        }*/
    }

    public void doProdFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        StringBuffer url;
        LavendelizeHttpServletRequest lavenderRequest;
        LavendelizeHttpServletResponse lavenderResponse;

        try {
            url = request.getRequestURL();
            URI requestURI = URI.create(url.toString());

            // use custom request and response objects
            lavenderRequest = new LavendelizeHttpServletRequest(request);
            lavenderResponse = new LavendelizeHttpServletResponse(response, processorFactory,
                    requestURI, request.getHeader("User-Agent"),
                    request.getContextPath() + "/", Gzip.canGzip(request));
            logRequest(url, request);
        } catch (RuntimeException re) {
            LOG.error("Error in Lavendelizer.doFilter()", re);
            throw re;
        }

        // continue the request
        // No exception handling at this point. Exceptions in processors are handled in LavendelizeOutputStream/Writer
        chain.doFilter(lavenderRequest, lavenderResponse);

        try {
            // close the response to make sure all buffers are flushed
            lavenderResponse.close();

            logResponse(url, lavenderResponse);
        } catch (IOException ioe) {
            LOG.error("Error in Lavendelizer.doFilter()", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            LOG.error("Error in Lavendelizer.doFilter()", re);
            throw re;
        }
    }

    private void logRequest(StringBuffer url, HttpServletRequest httpRequest) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering doFilter: url=" + url);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("  Request headers: ");
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = httpRequest.getHeader(key);
                LOG.trace("    " + key + ": " + value);
            }
        }
    }

    private void logResponse(StringBuffer url, LavendelizeHttpServletResponse lavendelResponse) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Leaving doFilter:  url=" + url);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("  Response headers: ");
            for (Entry<String, String> entry : lavendelResponse.getHeaders().entrySet()) {
                LOG.trace("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
    }

}
