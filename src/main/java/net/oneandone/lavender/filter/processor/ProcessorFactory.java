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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for two content types: text/html and text/css.
 */
public class ProcessorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorFactory.class);

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
