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

import net.oneandone.lavender.filter.processor.Processor;
import net.oneandone.lavender.filter.processor.ProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * An implementation of {@link HttpServletResponse} that uses a custom {@link ServletOutputStream} and
 * {@link PrintWriter} to lavendelize the response content.
 */
public class LavendelizeHttpServletResponse extends HttpServletResponseWrapper {
    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(LavendelizeHttpServletResponse.class);

    /** The processor factory. */
    protected final ProcessorFactory processorFactory;

    /** The URI of the HTTP request. */
    protected final URI requestURI;

    protected final String userAgent;

    /** The context path. Always starts and ends with a "/" */
    protected final String contextPath;

    /**
     * The headers. Also store them here for logging and testing - they're unavailable in the original
     * HttpServletResponseWrapper.
     */
    private Map<String, String> headers = new HashMap<>();

    /** The writer, lazy initialized when getWriter() is called. */
    protected PrintWriter writer;

    /** The output stream, lazy initialzed when getOutputStream() is called. */
    protected ServletOutputStream outputStream;

    /** Null until initialize has been called, otherwise indicates if a there's processor */
    private Boolean processing;

    /** Null if not set */
    private Integer contentLength;

    /**
     * Initialized to true, if the client can receive gzip. Reset to false by initialize if the contentType is
     * not enabled for compression.
     */
    private boolean gzip;

    public LavendelizeHttpServletResponse(HttpServletResponse response, ProcessorFactory processorFactory,
            URI requestURI, String userAgent, String contextPath, boolean clientCanGzip) {
        super(response);
        if (!contextPath.startsWith("/")) {
            throw new IllegalArgumentException(contextPath);
        }
        if (!contextPath.endsWith("/")) {
            throw new IllegalArgumentException(contextPath);
        }
        this.processorFactory = processorFactory;
        this.requestURI = requestURI;
        this.userAgent = userAgent;
        this.contextPath = contextPath;
        this.processing = null;
        this.contentLength = null;
        this.gzip = clientCanGzip;
    }

    /**
     * Gets the base URI.
     * @return the base URI
     */
    private URI getBaseURI() {
        // TODO: check for base URI in HTTP header
        // Content-Location = "Content-Location" ":"
        // ( absoluteURI | relativeURI )
        return requestURI;
    }

    private String getContextPath() {
        return contextPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintWriter getWriter() {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called.");
        }

        if (writer == null) {
            writer = new PrintWriter(new DeferredWriter() {
                @Override
                protected Writer createTarget() throws IOException {
                    String encoding;
                    Processor processor;
                    Writer target;

                    encoding = defineCharacterEncoding();
                    processor = initialize();
                    if (gzip) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("getWriter() -> gzipped original outputStream");
                        }
                        target = new OutputStreamWriter(new GZIPOutputStream(LavendelizeHttpServletResponse.super.getOutputStream()),
                                encoding);
                    } else {
                        target = LavendelizeHttpServletResponse.super.getWriter();
                    }
                    if (processor == null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("getWriter() -> original writer (gzip=" + gzip +")");
                        }
                        return target;
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("getWriter() -> lavendelized writer (gzip=" + gzip +")");
                        }
                        processor.setWriter(target);
                        return new LavendelizeWriter(processor);
                    }
                }
            });
        }

        return writer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called.");
        }
        if (outputStream == null) {
            outputStream = new DeferredOutputStream() {
                @Override
                protected OutputStream createTarget() throws IOException {
                    Processor processor;
                    String encoding;
                    OutputStream tmp;

                    processor = initialize();
                    if (processor == null) {
                        tmp = LavendelizeHttpServletResponse.super.getOutputStream();
                        if (gzip) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("getOutputStream() -> original outputStream");
                            }
                            tmp = new GZIPOutputStream(tmp);
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("getOutputStream() -> gzipped original outputStream");
                            }
                            // do nothing
                        }
                        return tmp;
                    } else {
                        encoding = defineCharacterEncoding();
                        if (gzip) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("getOutputStream() -> lavendelized gzipped original outputStream");
                            }
                            processor.setWriter(new OutputStreamWriter(
                                    new GZIPOutputStream(LavendelizeHttpServletResponse.super.getOutputStream()), encoding));
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("getOutputStream() -> lavendelized original writer");
                            }
                            processor.setWriter(LavendelizeHttpServletResponse.super.getWriter());
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Created WriterOutputStream with encoding " + encoding);
                        }
                        return WriterOutputStream.create(new LavendelizeWriter(processor), encoding);
                    }
                }
            };
        }

        return outputStream;
    }

    /**
     *  Explicitly set a proper character encoding - otherwise, we'd get one implicitly defined by getWriter()
     * (if system property "org.apache.catalina.STRICT_SERVLET_COMPLIANCE" if set to "true").
     */
    private String defineCharacterEncoding() {
        if (!hasCharacterEncoding()) {
            // TODO:
            // Servlets default to ISO, but html makes more sense in our environment.
            // For html, we should scan for <meta http-equiv="Content-Type" content="..."> tags
            setCharacterEncoding("UTF-8");
        }
        return getCharacterEncoding();
    }

    protected boolean hasCharacterEncoding() {
        String contentType;

        contentType = getContentType();
        return contentType != null && contentType.contains(";charset=");
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        if (outputStream != null) {
            outputStream.close();
        }
    }

    private static final List<String> GZIP_ENABLED = Arrays.asList("text/javascript", "text/css");

    /**
     * Creates the processor.
     * @return the processor, null if none is used for the response content type
     */
    public Processor initialize() {
        String contentType;
        Processor processor;
        MimeType mimeType;

        contentType = getContentType();
        if (contentType != null) {
            try {
                mimeType = new MimeType(contentType);
            } catch (MimeTypeParseException e) {
                LOG.warn("cannot parse mimeType: " + contentType, e);
                mimeType = null;
            }
        } else {
            mimeType = null;
        }
        if (contentType == null || mimeType == null) {
            processor = null;
            gzip = false;
        } else {
            String baseType = mimeType.getBaseType();
            baseType = baseType.toLowerCase();
            processor = processorFactory.createProcessor(baseType,  getBaseURI(), getContextPath());
            if (gzip) {
                gzip = GZIP_ENABLED.contains(baseType);
            }
        }
        processing = processor != null;
        if (contentLength != null) {
            super.setContentLength(processing || gzip ? -1 : contentLength);
        }
        if (gzip) {
            // see http://cs193h.stevesouders.com and "High Performance Websites", by Steve Souders
            setHeader("Content-Encoding", "gzip");
            addHeader("Vary", "Accept-Encoding");
        }
        return processor;
    }

    @Override
    public void setContentLength(int len) {
        if (processing != null) {
            super.setContentLength(processing || gzip ? -1 /* length changed by translation */: len);
        } else {
            contentLength = len;
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        headers.put(name, value);
    }

    @Override
    public void addDateHeader(String name, long date) {
        super.addDateHeader(name, date);
        headers.put(name, "" + date);
    }

    @Override
    public void addIntHeader(String name, int value) {
        super.addIntHeader(name, value);
        headers.put(name, "" + value);
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        headers.put(name, value);
    }

    @Override
    public void setDateHeader(String name, long date) {
        super.setDateHeader(name, date);
        headers.put(name, "" + date);
    }

    @Override
    public void setIntHeader(String name, int value) {
        super.setIntHeader(name, value);
        headers.put(name, "" + value);
    }

    //--

    @Override
    public void sendError(int sc) throws IOException {
        if (sc == 404) {
            resourceNotFoundWarning();
        }
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (sc == 404) {
            resourceNotFoundWarning();
        }
        super.sendError(sc, msg);
    }

    private void resourceNotFoundWarning() {
        if (isBot() || requestURI.getPath().endsWith("favicon.ico")) {
            return;
        }
        LOG.warn("resource not found: " + requestURI);
    }

    private boolean isBot() {
        if (userAgent == null) {
            return false;
        }

        for (String bot : BOTS) {
            if (userAgent.contains(bot)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] BOTS = { "Googlebot", "YandexBot", "Slurp", "bingbot", "Baiduspider", "msnbot", "ia_archiver",
            "MJ12bot", "Vagabondo", "UnisterBot" };
}
