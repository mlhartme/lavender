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
package net.oneandone.lavender.filter.it;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.Buffer;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs Lavendelizer in Tomcat.
 */
public class LavendelizerIT {
    private static final World WORLD = World.createMinimal();
    private static final FileNode HOME = WORLD.guessProjectHome(LavendelizerIT.class);

    private static final int PORT = 8087;
    private static Tomcat container;

    @BeforeAll
    public static void startTomcat() throws Exception {
        FileNode dest = HOME.join("target/testapp1");

        HOME.join("src/test/testapp1").copy(dest);
        container = new Tomcat();
        container.setBaseDir(dest.join("tomcat").getAbsolute());
        container.addWebapp("/", dest.join("webapp").getAbsolute());
        container.setPort(PORT);
        container.start();
    }

    @AfterAll
    public static void stopTomcat() throws Exception {
        if (container != null) {
            container.stop();
        }
    }

    //--

    @Test
    public void imageResource() throws Exception {
        doGet("logo.png");
    }

    @Test
    public void cssResourceWithoutRewrite() throws Exception {
        doGet("other.css");
    }

    @Test
    public void cssResourceWithRewrite() throws Exception {
        String content = doGet("main.css");
        assertTrue(content.contains("http://lavendel2.local/lavender/app/38077243c8f578b3ab92b3fc4754aba4-li.gif"));
    }

    @Test
    public void htmlWithRewrite() throws IOException {
        String content = doGet("page.html");

        assertTrue(content.contains("http://lavendel2.local/lavender/app/f13f1bdec0d20d173aeee2b90a2a12e3-main.css"));
        assertTrue(content.contains("http://lavendel3.local/lavender/app/4c57f05c2589e93e1b41fa3a971f8883-other.css"));
        assertTrue(content.contains("http://lavendel3.local/lavender/app/5c4990d0d465809ca15232cc7b190583-ie6.css"));
        assertTrue(content.contains("http://lavendel1.local/lavender/app/ae01e2f698490a0c137018c5dcf07d4c-ie7.css"));
        assertTrue(content.contains("http://lavendel1.local/lavender/app/75d5b048491003744336d32a78154449-logo.png"));
        assertTrue(content.contains("http://lavendel1.local/lavender/app/75d5b048491003744336d32a78154449-logo.png?param=value"));
        assertTrue(content.contains("http://lavendel1.local/lavender/app/e4ccf35257829c23b8e31e16619289ba-background.png"));
        assertTrue(content.contains("http://lavendel3.local/lavender/app/3dcdd67e7205534e2f7ad7c41683dc40-main.js"));

        assertTrue(content.endsWith("</html>"));
    }

    @Test
    public void htmlRewriteWithoutChanges() throws IOException {
        String content;

        content = doGet("encoding.html");
        assertTrue(content.contains("äöüÄÖÜß€µ"));
    }


    private String doGet(String path) throws IOException {
        String one;
        String two;

        one = doGetZ(path, false);
        two = doGetZ(path, true);
        assertEquals(one, two);
        return one;
    }

    private String doGetZ(String path, boolean gzip) throws IOException {
        URL url;
        HttpURLConnection conn;
        byte[] bytes;
        InputStream in;
        ByteArrayOutputStream out;

        url = new URL("http://localhost:" + PORT + "/" + path);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (gzip) {
            conn.addRequestProperty("Accept-Encoding", "gzip");
        }
        bytes = new Buffer().readBytes(conn.getInputStream());
        if ("gzip".equals(conn.getHeaderField("Content-Encoding"))) {
            in = new GZIPInputStream(new ByteArrayInputStream(bytes));
            out = new ByteArrayOutputStream();
            new Buffer().copy(in, out);
            bytes = out.toByteArray();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
