package net.oneandone.lavender.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by sfelis on 9/28/16.
 */
public class FilterListChain implements FilterChain {

    private final List<Filter> filters;

    private final FilterChain delegate;

    private int currentFilterIndex = 0;

    public FilterListChain(List<Filter> filters, FilterChain delegate) {
        this.filters = filters;
        this.delegate = delegate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        if (currentFilterIndex < filters.size()) {
            filters.get(currentFilterIndex++).doFilter(request, response, this);
        } else {
            delegate.doFilter(request, response);
        }
    }
}
