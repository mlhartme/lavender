package com.oneandone.lavendel.publisher.pustefix;

import com.oneandone.lavendel.publisher.Resource;
import com.oneandone.lavendel.publisher.config.Filter;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PustefixExtractorTest {

    @Test
    public void testExtract() throws Exception {
        URL url = getClass().getClassLoader().getResource("dummy.war");

        Map<String, Resource> resources = new HashMap<>();
        PustefixExtractor extractor = new PustefixExtractor(new Filter(), new File(url.toURI()));
        for (Resource resource : extractor) {
            resources.put(resource.getPath(), resource);
        }

        assertEquals(14, resources.size());

        assertTrue(resources.containsKey("img/sub/check_grey.gif"));
        assertTrue(resources.containsKey("img/sub/check_green.gif"));
        assertTrue(resources.containsKey("img/btn_weiter.gif"));
        assertTrue(resources.containsKey("modules/stageassistent/img/open.gif"));
        assertTrue(resources.containsKey("modules/stageassistent/img/minimize.gif"));
        assertTrue(resources.containsKey("modules/stageassistent/img/close.gif"));
        assertTrue(resources.containsKey("modules/frontend-tools/img/delete.png"));
        assertTrue(resources.containsKey("modules/frontend-tools/img/accept.png"));
        assertTrue(resources.containsKey("modules/frontend-tools/img/cross.png"));
        assertTrue(resources.containsKey("modules/frontend-elements/style/error.css"));
        assertTrue(resources.containsKey("modules/frontend-elements/img/buttons/buttons-ie6.png"));
        assertTrue(resources.containsKey("modules/frontend-elements/img/buttons/buttons.png"));
        assertTrue(resources.containsKey("modules/frontend-elements/img/error/error.gif"));
        assertTrue(resources.containsKey("modules/frontend-elements/img/error/logo.gif"));
    }

    @Ignore
    @Test
    public void testPerformance() throws JAXBException, IOException {

        URL url = getClass().getClassLoader().getResource("dummy.war");

        int num = 10000;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            ZipInputStream zipIn = new ZipInputStream(url.openStream());
            countEntries(zipIn);
        }
        long t1 = System.currentTimeMillis();
        long t = t1 - t0;
        System.out.println(num + " in " + t + "ms");

    }

    private void countEntries(ZipInputStream zipIn) throws IOException {
        int count = 0;
        while (zipIn.getNextEntry() != null) {
            count++;
        }
        assertEquals(32, count);
    }

}
