package com.oneandone.lavendel.publisher;

import com.oneandone.lavendel.index.Index;
import net.oneandone.sushi.fs.Node;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class DistributorTest {

    @Test
    public void write() throws IOException {
        Index index;

        Resource resource1 = new Resource("abcd".getBytes("UTF-8"), "img/test.png", "folder");
        Resource resource2 = new Resource("abcd".getBytes("UTF-8"), "modules/stageassistent/img/test.gif", "stageassistent");
        Distributor storage = new Distributor(new HashMap<Node, Node>(), new Index());
        storage.write(resource1.labelLavendelized(""), resource1.getData());
        storage.write(resource2.labelLavendelized(""), resource1.getData());
        index = storage.close();
        assertEquals("e2f/c714c4727ee9395f324cd2e7f331f/folder/test.png", index.lookup("img/test.png").getLavendelizedPath());
        assertEquals("e2f/c714c4727ee9395f324cd2e7f331f/stageassistent/test.gif",
                index.lookup("modules/stageassistent/img/test.gif").getLavendelizedPath());
    }

}
