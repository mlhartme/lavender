package net.oneandone.lavender.filter.processor;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
