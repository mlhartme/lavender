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
package net.oneandone.lavender.filter.pt;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

public class MockHttpServletResponse implements HttpServletResponse {
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    private StringWriter result = new StringWriter();

    public String getResult() {
        return result.getBuffer().toString();
    }

    public void addCookie(Cookie cookie) {
    }

    public void addDateHeader(String name, long date) {
    }

    public void addHeader(String name, String value) {
    }

    public void addIntHeader(String name, int value) {
    }

    public boolean containsHeader(String name) {
        return false;
    }

    public String encodeRedirectURL(String url) {
        return null;
    }

    public String encodeRedirectUrl(String url) {
        return null;
    }

    public String encodeURL(String url) {
        return null;
    }

    public String encodeUrl(String url) {
        return null;
    }

    public void sendError(int sc) throws IOException {
    }

    public void sendError(int sc, String msg) throws IOException {
    }

    public void sendRedirect(String location) throws IOException {
    }

    public void setDateHeader(String name, long date) {
    }

    public void setHeader(String name, String value) {
    }

    public void setIntHeader(String name, int value) {
    }

    public void setStatus(int sc) {
    }

    public void setStatus(int sc, String sm) {
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public String getHeader(String s) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    public void flushBuffer() throws IOException {
    }

    public int getBufferSize() {
        return 0;
    }

    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public String getContentType() {
        return "text/html; charset=UTF-8";
    }

    public Locale getLocale() {
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                public void write(int b) throws IOException {
                    result.append((char) b);
                }
            };
        }
        return outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(result);
        }
        return writer;
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
    }

    public void resetBuffer() {
    }

    public void setBufferSize(int size) {
    }

    public void setCharacterEncoding(String charset) {
    }

    public void setContentLength(int len) {
    }

    public void setContentType(String type) {
    }

    public void setLocale(Locale loc) {
    }

}
