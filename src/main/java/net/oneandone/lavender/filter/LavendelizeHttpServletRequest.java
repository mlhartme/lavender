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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * An implementation of {@link HttpServletRequest} that removes the "Accept-Encoding" header from the request.
 * This is necessary to avoid gzipped output (e.g. by Jasmin), which had to be unzipped by Lavender.
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
        List<String> list = new ArrayList<>();

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
    public Enumeration getHeaders(String name) {
        if (ACCEPT_ENCODING.equalsIgnoreCase(name)) {
            return Collections.enumeration(Collections.emptyList());
        }

        return super.getHeaders(name);
    }

}
