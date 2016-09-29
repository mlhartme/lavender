package net.oneandone.lavender.filter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by sfelis on 9/28/16.
 */
public class FilterListChainTest {

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
    public void doFilterShouldCallOriginalFilterChain() throws Exception {
        FilterListChain filterListChain = new FilterListChain(filters, chainMock);
        doAnswer(mockDoFilterCall()).when(filterAMock).doFilter(any(), any(), any());
        doAnswer(mockDoFilterCall()).when(filterBMock).doFilter(any(), any(), any());


        filterListChain.doFilter(requestMock, responseMock);


        verify(filterAMock, times(1)).doFilter(eq(requestMock), eq(responseMock), eq(filterListChain));
        verify(filterBMock, times(1)).doFilter(eq(requestMock), eq(responseMock), eq(filterListChain));
        verify(chainMock, times(1)).doFilter(eq(requestMock), eq(responseMock));
    }

    @Test
    public void doFilterShouldNotCallOriginalFilterChain() throws Exception {
        FilterListChain filterListChain = new FilterListChain(filters, chainMock);
        doAnswer(mockDoFilterCall()).when(filterAMock).doFilter(any(), any(), any());


        filterListChain.doFilter(requestMock, responseMock);


        verify(filterAMock, times(1)).doFilter(eq(requestMock), eq(responseMock), eq(filterListChain));
        verify(filterBMock, times(1)).doFilter(eq(requestMock), eq(responseMock), eq(filterListChain));
        verify(chainMock, times(0)).doFilter(eq(requestMock), eq(responseMock));
    }

    @Test
    public void doFilterShouldNotCallFilterB() throws Exception {
        FilterListChain filterListChain = new FilterListChain(filters, chainMock);


        filterListChain.doFilter(requestMock, responseMock);


        verify(filterAMock, times(1)).doFilter(eq(requestMock), eq(responseMock), eq(filterListChain));
        verify(filterBMock, times(0)).doFilter(eq(requestMock), eq(responseMock), eq(filterListChain));
        verify(chainMock, times(0)).doFilter(eq(requestMock), eq(responseMock));
    }

    private Answer mockDoFilterCall() {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ServletRequest request = (ServletRequest) invocationOnMock.getArguments()[0];
                ServletResponse response = (ServletResponse) invocationOnMock.getArguments()[1];
                FilterChain chain = (FilterChain) invocationOnMock.getArguments()[2];
                chain.doFilter(request, response);
                return null;
            }
        };
    }

}