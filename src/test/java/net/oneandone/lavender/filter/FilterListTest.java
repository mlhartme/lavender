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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by sfelis on 9/28/16.
 */
public class FilterListTest {

    @Mock
    private FilterConfig filterConfigMock;

    @Mock
    private Filter filterAMock;

    @Mock
    private Filter filterBMock;

    @Mock
    private ServletRequest requestMock;

    @Mock
    private ServletResponse responseMock;

    @Mock
    private FilterChain chainMock;

    @Mock
    private FilterListChain filterListChainMock;

    private List<Filter> filters;

    @Before
    public void setUp () {
        MockitoAnnotations.initMocks(this);

        filters = new ArrayList<>();
        filters.add(filterAMock);
        filters.add(filterBMock);
    }

    @Test
    public void init() throws ServletException {
        FilterList filterList = new FilterList(filters);


        filterList.init(filterConfigMock);


        verify(filterAMock, times(1)).init(eq(filterConfigMock));
        verify(filterBMock, times(1)).init(eq(filterConfigMock));
    }

    @Test
    public void doFilter() throws IOException, ServletException {
        FilterList filterList = new FilterList(filters);
        FilterList filterListSpy = Mockito.spy(filterList);
        when(filterListSpy.createFilterListChain(any())).thenReturn(filterListChainMock);


        filterListSpy.doFilter(requestMock, responseMock, chainMock);


        verify(filterListChainMock, times(1)).doFilter(eq(requestMock), eq(responseMock));
    }

    @Test
    public void doFilterWithOneFilterShouldCallFilterDirectly() throws IOException, ServletException {
        filters = new ArrayList<>();
        filters.add(filterAMock);
        FilterList filterList = new FilterList(filters);
        FilterList filterListSpy = Mockito.spy(filterList);


        filterListSpy.doFilter(requestMock, responseMock, chainMock);


        verify(filterAMock, times(1)).doFilter(eq(requestMock), eq(responseMock), eq(chainMock));
    }

    @Test
    public void destroy() throws ServletException {
        FilterList filterList = new FilterList(filters);


        filterList.destroy();


        verify(filterAMock, times(1)).destroy();
        verify(filterBMock, times(1)).destroy();
    }
}