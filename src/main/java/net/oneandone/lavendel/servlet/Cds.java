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
package net.oneandone.lavendel.servlet;

import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Cds extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Cds.class);
    private static final String DOCBASE_PARAM = "docbase";
    private static final int INPUTSTREAM_BUFFER = 4096;
    private static final int RESPONSE_BUFFER = 4096;

    private File docbase;

    @Override
    public void init() throws ServletException {
        String path;

        path = getInitParameter(DOCBASE_PARAM);
        if (path == null) {
            throw new ServletException("missing init parameter " + DOCBASE_PARAM
                    + " that points to the directory where static resources are loaded from.");
        }
        docbase = new File(path);
        if (!docbase.isDirectory()) {
            throw new ServletException("docbase is not a directory: " + docbase);

        }
        LOG.info("Using docbase " + docbase);
    }

    public void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        header(request, response);
    }

    public void doGet(HttpServletRequest request,  HttpServletResponse response) throws IOException {
        File file;

        file = header(request, response);
        if (file != null) {
            content(file, response);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    private File header(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path;
        String contentType;
        long contentLength;
        File file;

        path = request.getPathInfo();
        if (path == null) {
            path = "";
        } else {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        file = new File(docbase, path);
        if (!file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURI());
            return null;
        }
        setCacheExpireDate(response, 10);
        response.setDateHeader("Last-Modified", file.lastModified());
        contentType = getServletContext().getMimeType(file.getName());
        if (contentType != null) {
            response.setContentType(contentType);
        }
        contentLength = file.length();
        if (contentLength >= Integer.MAX_VALUE) {
            throw new IOException("file to big: " + contentLength);
        }
        response.setContentLength((int) contentLength);
        LOG.info(file + ": contentType=" + contentType + ", contentLength=" + contentLength);
        return file;
    }

    private void content(File file, HttpServletResponse response) throws IOException {
        ServletOutputStream out;

        out = response.getOutputStream();
        try {
            response.setBufferSize(RESPONSE_BUFFER);
        } catch (IllegalStateException e) {
            // Silent catch
        }
        copy(file, out);
    }

    private void copy(File src, ServletOutputStream ostream) throws IOException {
        InputStream in;
        byte[] buffer;
        int len;

        buffer = new byte[INPUTSTREAM_BUFFER];
        in = new FileInputStream(src);
        try {
            while (true) {
                len = in.read(buffer);
                if (len == -1) {
                    break;
                }
                ostream.write(buffer, 0, len);
            }
        } finally {
            in.close();
        }
    }

    private static void setCacheExpireDate(HttpServletResponse response, int years) {
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.roll(Calendar.YEAR, years);
        long seconds = (cal.getTimeInMillis() - now) / 1000;
        response.setHeader("Cache-Control", "PUBLIC, max-age=" + seconds + ", strict-revalidate");
        response.setHeader("Expires", EXPIRES_FORMAT.format(cal.getTime()));
    }

    private static final DateFormat EXPIRES_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
}
