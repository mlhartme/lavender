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

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UriHelperTest {

    @Test
    public void resolveNormal() {
        assertEquals("img/close.gif", doResolve("img/close.gif", "http://localhost:80", "/"));
    }

    @Test
    public void resolveNullPath() {
        assertNull(doResolve("mailto:michael.hartmeier@1und1.de", "http://localhost:80", "/"));
    }

    @Test
    public void resolveRelativeReferenceRootcontext() {
        assertEquals("img/close.gif", doResolve("img/close.gif", "http://localhost:80/", "/"));
    }

    @Test
    public void resolveAbsoluteReferenceRootContext() {
        assertEquals("img/close.gif", doResolve("/img/close.gif", "http://localhost:80/", "/"));
    }

    @Test
    public void resolveRelativeReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("img/close.gif", "http://localhost:80/app/", "/app/"));
    }

    @Test
    public void resolveAbsoluteReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("/app/img/close.gif", "http://localhost:80/app/", "/app/"));
    }

    @Test
    public void resolveChildRelativeReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("close.gif", "http://localhost:80/app/img/", "/app/"));
    }

    @Test
    public void resolveParentRelativeReferenceSubContext() {
        assertEquals("img/close.gif", doResolve("../img/close.gif", "http://localhost:80/app/img/", "/app/"));
    }

    private String doResolve(String reference, String baseUri, String contextPath) {
        return UriHelper.resolvePathWithoutContext(URI.create(reference), URI.create(baseUri), contextPath);
    }
}
