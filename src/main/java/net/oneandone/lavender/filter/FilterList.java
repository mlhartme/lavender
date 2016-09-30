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
package net.oneandone.lavender.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds a list of servlet filters which are called in a chain. The execution order is the list order
 */
public class FilterList implements Filter {

    private final List<Filter> filters;

    public FilterList(final List<Filter> filters) {
        if (filters == null) {
            throw new NullPointerException("Filter list must not be null");
        } else if (filters.isEmpty()) {
            throw new NullPointerException("Filter list must not be empty");
        }
        this.filters = new CopyOnWriteArrayList<>(filters);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        for (Filter filter : filters) {
            filter.init(filterConfig);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (filters.size() == 1) {
            filters.get(0).doFilter(request, response, chain);
        } else {
            createFilterListChain(chain).doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        for (Filter filter : filters) {
            filter.destroy();
        }
    }

    public List<Filter> getFilters() {
        return new ArrayList<>(filters);
    }

    FilterListChain createFilterListChain(FilterChain chain) {
        return new FilterListChain(filters, chain);
    }

}
