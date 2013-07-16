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
package net.oneandone.lavender.filter.it;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Runs Lavendelizer in Tomcat.
 */
public class LavendelizerIT {
    private static final World WORLD = new World();
    private static final FileNode HOME = WORLD.guessProjectHome(LavendelizerIT.class);

    private static final int PORT = 8087;
    private static final String HOST = "127.0.0.1";
    private static Embedded container;

    @BeforeClass
    public static void startTomcat() throws Exception {
        FileNode dest = HOME.join("target/testapp1");

        HOME.join("src/test/testapp1").copy(dest);
        container = new Embedded();
        doStartTomcat(dest.toPath().toFile(), HOST, PORT);
    }

    private static void doStartTomcat(File dir, String host, int port) throws Exception {
        container.setCatalinaHome(new File(dir, "tomcat").getAbsolutePath());
        container.setRealm(new MemoryRealm());

        // create webapp loader
        WebappLoader loader = new WebappLoader(LavendelizerIT.class.getClassLoader());

        loader.addRepository(new File("target/classes").toURI().toURL().toString());

        // create context
        Context rootContext = container.createContext("/", new File(dir, "webapp").getAbsolutePath());
        rootContext.setLoader(loader);
        rootContext.setReloadable(false);

        // create host
        Host localHost = container.createHost(host, dir.getAbsolutePath());
        localHost.addChild(rootContext);

        // create engine
        Engine engine = container.createEngine();
        engine.setName("localEngine");
        engine.addChild(localHost);
        engine.setDefaultHost(localHost.getName());
        container.addEngine(engine);

        // create http connector
        Connector httpConnector = container.createConnector((InetAddress) null, port, false);
        container.addConnector(httpConnector);

        container.setAwait(true);

        // start server
        container.start();
    }

    @AfterClass
    public static void stopTomcat() throws Exception {
        if (container != null) {
            container.stop();
        }
    }

    //--

    @Test
    public void imageResource() throws Exception {
        getSame("logo.png");
    }

    @Test
    public void cssResourceWithoutRewrite() throws Exception {
        getSame("other.css");
    }

    @Test
    public void cssResourceWithRewrite() throws Exception {
        String content = get("main.css");
        assertTrue(content.contains("http://lavendel2.local/lavendel/app/38077243c8f578b3ab92b3fc4754aba4-li.gif"));
    }

    @Test
    public void htmlWithRewrite() throws IOException {
        String content = get("page.html");

        assertTrue(content.contains("http://lavendel2.local/lavendel/app/f13f1bdec0d20d173aeee2b90a2a12e3-main.css"));
        assertTrue(content.contains("http://lavendel3.local/lavendel/app/4c57f05c2589e93e1b41fa3a971f8883-other.css"));
        assertTrue(content.contains("http://lavendel3.local/lavendel/app/5c4990d0d465809ca15232cc7b190583-ie6.css"));
        assertTrue(content.contains("http://lavendel1.local/lavendel/app/ae01e2f698490a0c137018c5dcf07d4c-ie7.css"));
        assertTrue(content.contains("http://lavendel1.local/lavendel/app/75d5b048491003744336d32a78154449-logo.png"));
        assertTrue(content.contains("http://lavendel1.local/lavendel/app/e4ccf35257829c23b8e31e16619289ba-background.png"));
        assertTrue(content.contains("http://lavendel3.local/lavendel/app/3dcdd67e7205534e2f7ad7c41683dc40-main.js"));

        assertTrue(content.endsWith("</html>"));
    }

    @Test
    public void htmlRewriteWithoutChanges() throws IOException {
        String content;

        content = getSame("encoding.html");
        assertTrue(content, content.contains("äöüÄÖÜß€µ"));
    }


    private String get(String path) throws IOException {
        return doGet(path, false);
    }

    private String getSame(String path) throws IOException {
        return doGet(path, true);
    }

    private String doGet(String path, boolean same) throws IOException {
        String one;
        String two;

        one = doGet(path, same, false);
        two = doGet(path, same, true);
        assertEquals(one, two);
        return one;
    }

    private String doGet(String path, boolean same, boolean gzip) throws IOException {
        DefaultHttpClient client;
        HttpGet get;
        HttpResponse response;
        HttpEntity entity;
        byte[] content;
        String charset;

        client = new DefaultHttpClient();
        get = new HttpGet("http://localhost:" + PORT + "/" + path);
        if (gzip) {
            get.setHeader("Accept-Encoding", "gzip");
        }
        response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
        System.out.println(path + ": " + Arrays.asList(response.getHeaders("content-type")));
        entity = response.getEntity();
        content = EntityUtils.toByteArray(entity);
        if (isGzip(response)) {
            content = gunzip(content);
        }
        if (same) {
            assertArrayEquals(content, HOME.join("src/test/testapp1/webapp", path).readBytes());
        }
        charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }
        return new String(content, charset);
    }

    private boolean isGzip(HttpResponse response) {
        Header[] headers;

        headers = response.getHeaders("Content-Encoding");
        for (Header header : headers) {
            if (header.getValue().equals("gzip")) {
                return true;
            }
        }
        return false;
    }

    private byte[] gunzip(byte[] bytes) throws IOException {
        InputStream in;
        ByteArrayOutputStream out;

        in = new GZIPInputStream(new ByteArrayInputStream(bytes));
        out = new ByteArrayOutputStream();
        WORLD.getBuffer().copy(in, out);
        in.close();
        out.close();
        return out.toByteArray();
    }
}
