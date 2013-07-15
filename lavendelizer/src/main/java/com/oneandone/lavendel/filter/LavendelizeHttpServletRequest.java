package com.oneandone.lavendel.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * An implementation of {@link HttpServletRequest} that removes the "Accept-Encoding" header from the request.
 * This is necessary to avoid gzipped output (e.g. by Jasmin), which had to be unzipped by Lavendel.
 * @author seelmann
 */
public class LavendelizeHttpServletRequest extends HttpServletRequestWrapper {

    private static final String ACCEPT_ENCODING = "Accept-Encoding";

    public LavendelizeHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getHeader(String name) {
        if (ACCEPT_ENCODING.equalsIgnoreCase(name)) {
            return null;
        }

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> list = new ArrayList<String>();

        HttpServletRequest request = (HttpServletRequest) getRequest();
        Enumeration<String> e = request.getHeaderNames();
        while (e.hasMoreElements()) {
            String n = e.nextElement();
            if (!ACCEPT_ENCODING.equalsIgnoreCase(n)) {
                list.add(n);
            }
        }

        Enumeration<String> en = Collections.enumeration(list);
        return en;
    }

    @Override
    public Enumeration<?> getHeaders(String name) {
        if (ACCEPT_ENCODING.equalsIgnoreCase(name)) {
            return Collections.enumeration(Collections.emptyList());
        }

        return super.getHeaders(name);
    }

}
