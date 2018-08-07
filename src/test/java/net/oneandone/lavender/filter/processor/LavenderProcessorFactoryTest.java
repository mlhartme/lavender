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
package net.oneandone.lavender.filter.processor;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LavenderProcessorFactoryTest {

    protected LavenderProcessorFactory processorFactory;

    @Before
    public void setUp() {
        processorFactory = new LavenderProcessorFactory(null);
    }

    @Test
    public void testHtml() {
        Processor htmlProcessor = processorFactory.createProcessor("text/html", null, "/");
        assertNotNull(htmlProcessor);
        assertTrue(htmlProcessor instanceof HtmlProcessor);
    }

    @Test
    public void testCss() {
        Processor cssProcessor = processorFactory.createProcessor("text/css", null, "/");
        assertNotNull(cssProcessor);
        assertTrue(cssProcessor instanceof CssProcessor);
    }

    @Test
    public void testUnknownContentType() {
        Processor nullProcessor = processorFactory.createProcessor("image/png", null, "/");
        assertNull(nullProcessor);
    }

    @Test
    public void testNullContentType() {
        Processor nullProcessor = processorFactory.createProcessor(null, null, "/");
        assertNull(nullProcessor);
    }

    @Test
    public void testIllegalContentType() {
        Processor nullProcessor = processorFactory.createProcessor("<%$=/>", null, "/");
        assertNull(nullProcessor);
    }

}
