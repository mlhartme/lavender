package net.oneandone.lavendel.filter.it;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

public class ErrorToolNGTest {

    private static final Logger LOG = Logger.getLogger(ErrorToolNGTest.class);

    @Ignore
    @Test
    public void testErrorToolNG() throws Exception {
        LOG.error("This is my error message.", new RuntimeException("abc"));
        // give ActiveMQ some time to send the messages
        Thread.sleep(10000);
    }

}
