package com.oneandone.lavendel.filter;

import com.oneandone.lavendel.index.Index;
import com.oneandone.lavendel.processor.ProcessorFactory;
import com.oneandone.lavendel.rewrite.RewriteEngine;
import com.oneandone.lavendel.rewrite.UrlCalculator;
import org.apache.log4j.Logger;

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
import java.util.Map.Entry;

/**
 * A servlet filter that <em>lavendelizes</em> the response content.
 * @author seelmann
 */
public class Lavendelizer implements Filter {
    private static final Logger LOG = Logger.getLogger(Lavendelizer.class);

    /** The filter configuration. */
    protected FilterConfig filterConfig;

    protected ProcessorFactory processorFactory;

    public static final String LAVENDEL_IDX = "/WEB-INF/lavendel.idx";
    public static final String LAVENDEL_NODES = "/WEB-INF/lavendel.nodes";

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig config) throws ServletException {
        Index index;
        UrlCalculator urlCalculator;
        RewriteEngine rewriteEngine;

        try {
            this.filterConfig = config;

            index = new Index(resource(LAVENDEL_IDX));
            urlCalculator = new UrlCalculator(resource(LAVENDEL_NODES));
            rewriteEngine = new RewriteEngine(index, urlCalculator);

            this.processorFactory = new ProcessorFactory(rewriteEngine);
        } catch (IOException ie) {
            LOG.fatal("Error in Lavendelizer.init()", ie);
            throw new ServletException("io error", ie);
        } catch (ServletException se) {
            LOG.fatal("Error in Lavendelizer.init()", se);
            throw se;
        } catch (RuntimeException re) {
            LOG.fatal("Error in Lavendelizer.init()", re);
            throw re;
        }
    }

    private URL resource(String path) throws ServletException {
        URL url;

        try {
            url = filterConfig.getServletContext().getResource(path);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        if (url == null) {
            throw new ServletException("resource not found: " + path);
        }
        return url;
    }

    /**
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        StringBuffer url;
        LavendelizeHttpServletRequest lavendelRequest;
        LavendelizeHttpServletResponse lavendelResponse;

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            url = httpRequest.getRequestURL();
            URI requestURI = URI.create(url.toString());

            // use custom request and response objects
            lavendelRequest = new LavendelizeHttpServletRequest(httpRequest);
            lavendelResponse = new LavendelizeHttpServletResponse(httpResponse, processorFactory,
                    requestURI, httpRequest.getHeader("User-Agent"),
                    httpRequest.getContextPath() + "/", Gzip.canGzip(httpRequest));
            logRequest(url, httpRequest);
        } catch (RuntimeException re) {
            LOG.fatal("Error in Lavendelizer.doFilter()", re);
            throw re;
        }

        // continue the request
        // No exception handling at this point. Exceptions in processors are handled in LavendelizeOutputStream/Writer
        chain.doFilter(lavendelRequest, lavendelResponse);

        try {
            // close the response to make sure all buffers are flushed
            lavendelResponse.close();

            logResponse(url, lavendelResponse);
        } catch (IOException ioe) {
            LOG.fatal("Error in Lavendelizer.doFilter()", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            LOG.fatal("Error in Lavendelizer.doFilter()", re);
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
