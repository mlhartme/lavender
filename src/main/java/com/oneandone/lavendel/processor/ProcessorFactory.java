package com.oneandone.lavendel.processor;

import com.oneandone.lavendel.rewrite.RewriteEngine;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for two content types: text/html and text/css.
 * @author seelmann
 */
public class ProcessorFactory {

    private static final Logger LOG = Logger.getLogger(ProcessorFactory.class);

    protected final RewriteEngine rewriteEngine;
    protected final Map<String, Class<? extends Processor>> contentTypes;

    public ProcessorFactory(RewriteEngine rewriteEngine) {
        this.rewriteEngine = rewriteEngine;

        contentTypes = new HashMap<String, Class<? extends Processor>>();
        contentTypes.put("text/html", HtmlProcessor.class);
        contentTypes.put("text/css", CssProcessor.class);

        LOG.info("Created default processor factory with content types: " + contentTypes);
    }

    public Processor createProcessor(String baseContentType, URI baseURI, String contextPath) throws IOException {
        try {
            if (contentTypes.containsKey(baseContentType)) {
                Class<? extends Processor> clazz = contentTypes.get(baseContentType);
                Processor processor = clazz.newInstance();
                processor.setRewriteEngine(rewriteEngine, baseURI, contextPath);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Created processor " + clazz.getSimpleName() + " for content type '" + baseContentType + "'");
                }
                return processor;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No processor created for content type '" + baseContentType + "'");
                }
                return null;
            }
        } catch (InstantiationException e) {
            LOG.error("Cannot instantiate processor", e);
            throw new IOException("Cannot instantiate processor", e);
        } catch (IllegalAccessException e) {
            LOG.error("Cannot instantiate processor", e);
            throw new IOException("Cannot instantiate processor", e);
        }
    }

}
