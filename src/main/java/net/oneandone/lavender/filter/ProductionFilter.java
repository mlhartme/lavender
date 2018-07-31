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
package net.oneandone.lavender.filter;

import net.oneandone.lavender.filter.processor.LavenderProcessorFactory;
import net.oneandone.lavender.filter.processor.LavenderRewriteEngine;
import net.oneandone.lavender.filter.processor.RewriteEngine;
import net.oneandone.lavender.index.Index;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
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
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;

/**
 * Created by awiegant on 04.03.16.
 */
public class ProductionFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ProductionFilter.class);

    World world;
    LavenderProcessorFactory processorFactory;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            world = World.create(false);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServletException("Could not initialize Lavender production filter", e);
        }
        Node webapp = world.file(filterConfig.getServletContext().getRealPath(""));

        Node indexSource = webapp.join(Lavender.LAVENDER_IDX);
        Node nodesSource = webapp.join(Lavender.LAVENDER_NODES);
        try {
            Index index = Index.load(indexSource);
            RewriteEngine rewriteEngine = LavenderRewriteEngine.load(index, nodesSource);
            processorFactory = new LavenderProcessorFactory(rewriteEngine);
            LOG.info("Lavender prod filter");
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServletException("Could not initialize Lavender production filter", e);
        }

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws IOException, ServletException {
        StringBuffer url;
        LavendelizeHttpServletRequest lavenderRequest;
        LavendelizeHttpServletResponse lavenderResponse;

        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            url = request.getRequestURL();
            URI requestURI = URI.create(url.toString());

            // use custom request and response objects
            lavenderRequest = new LavendelizeHttpServletRequest(request);
            lavenderResponse = new LavendelizeHttpServletResponse(response, processorFactory,
                    requestURI, request.getHeader("User-Agent"), request.getContextPath() + "/", Gzip.canGzip(request));
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
        } catch (IOException | RuntimeException e) {
            LOG.error("Error in Lavendelizer.doFilter()", e);
            throw e;
        }
    }

    @Override
    public void destroy() {

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
            for (Map.Entry<String, String> entry : lavendelResponse.getHeaders().entrySet()) {
                LOG.trace("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

}
