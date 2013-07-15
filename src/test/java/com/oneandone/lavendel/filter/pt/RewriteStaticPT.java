package com.oneandone.lavendel.filter.pt;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Runs a performance test.
 * @author seelmann
 */
@Ignore
public class RewriteStaticPT {
    private static final int PORT_WITHOUT_FILTER = 8089;
    private static final int PORT_WITH_FILTER = 8087;
    private static final String HOST_WITHOUT_FILTER = "127.0.0.2";
    private static final String HOST_WITH_FILTER = "127.0.0.1";
    private static Embedded containerWithoutFilter;
    private static Embedded containerWithFilter;

    @BeforeClass
    public static void startTomcats() throws Exception {

        String base = RewriteStaticPT.class.getResource("/").getFile();

        File testapp1 = new File(base, "testapp1");

        File testwithoutfilter = new File(base, "testwithoutfilter");
        FileUtils.copyDirectory(testapp1, testwithoutfilter);
        FileUtils.copyFile(new File(testwithoutfilter, "webapp/WEB-INF/web.xml.withoutfilter"), new File(
                testwithoutfilter, "webapp/WEB-INF/web.xml"));
        containerWithoutFilter = new Embedded();
        startTomcat(containerWithoutFilter, testwithoutfilter, HOST_WITHOUT_FILTER, PORT_WITHOUT_FILTER);

        File testwithfilter = new File(base, "testwithfilter");
        FileUtils.copyDirectory(testapp1, testwithfilter);
        FileUtils.copyFile(new File(testwithfilter, "webapp/WEB-INF/web.xml.withfilterindex"), new File(testwithfilter,
                "webapp/WEB-INF/web.xml"));
        containerWithFilter = new Embedded();
        startTomcat(containerWithFilter, testwithfilter, HOST_WITH_FILTER, PORT_WITH_FILTER);
    }

    private static void startTomcat(Embedded container, File dir, String host, int port) throws Exception {

        container.setCatalinaHome(new File(dir, "tomcat").getAbsolutePath());
        container.setRealm(new MemoryRealm());

        // create webapp loader
        WebappLoader loader = new WebappLoader(RewriteStaticPT.class.getClassLoader());

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
    public static void stopTomcats() throws Exception {
        // if (containerWithoutFilter != null) {
        // containerWithoutFilter.stop();
        // }
        // if (containerWithFilter != null) {
        // containerWithFilter.stop();
        // }
    }

    @Test
    public void runHttpClientMultiThreadedPT() throws Exception {
        for (int i = 0; i < 2; i++) {
            runHttpClientMultiThreadedTest(PORT_WITHOUT_FILTER, "without filter");
            runHttpClientMultiThreadedTest(PORT_WITH_FILTER, "with filter");
        }
    }

    private void runHttpClientMultiThreadedTest(int port, String message) throws Exception {

        int numLoops = 10000;
        for (int numThreads = 1; numThreads <= 10; numThreads++) {
            AtomicInteger counter = new AtomicInteger();
            String url = "http://localhost:" + port + "/page.html";
            int contentLength = 6065;
            // String url = "http://localhost:"+port+"/vi_login_now.html";

            DefaultHttpClient client = new DefaultHttpClient();

            // fetch a non-filtered resource for tomcat warmup
            HttpGet get = new HttpGet("http://localhost:" + port + "/background.png");
            HttpResponse response = client.execute(get);
            response.getEntity().consumeContent();

            executeGet(client, url);
            executeGet(client, url);
            executeGet(client, url);

            long t0 = System.currentTimeMillis();
            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new Runner(numLoops, counter, url));
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
            long t1 = System.currentTimeMillis();

            assertEquals(numLoops * numThreads, counter.get());

            long t = t1 - t0;
            long reqPerSec = (1000L * numLoops * numThreads) / t;
            long charsPerSec = (1000L * numLoops * numThreads * contentLength) / t;
            System.out.printf(
                    "HTTP Client multi-threaded performance test " + message
                            + ": %d threads, %d ms, %,d characters/s, %,d requests/s", numThreads, t, charsPerSec,
                    reqPerSec).println();
        }
    }

    private void executeGet(DefaultHttpClient client, String url) throws IOException {
        HttpGet get;
        HttpResponse response;
        get = new HttpGet(url);
        response = client.execute(get);
        response.getEntity().consumeContent();
    }

    class Runner implements Runnable {
        private int numLoops;
        private AtomicInteger counter;
        private String url;
        private DefaultHttpClient client;

        public Runner(int numLoops, AtomicInteger counter, String url) {
            this.numLoops = numLoops;
            this.counter = counter;
            this.url = url;
            this.client = new DefaultHttpClient();
        }

        public void run() {
            try {
                for (int i = 0; i < numLoops; i++) {
                    executeGet(client, url);
                    counter.incrementAndGet();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

}
