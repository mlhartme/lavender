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

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorToolNGTest {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorToolNGTest.class);

    @Ignore
    @Test
    public void testErrorToolNG() throws Exception {
        LOG.error("This is my error message.", new RuntimeException("abc"));
        // give ActiveMQ some time to send the messages
        Thread.sleep(10000);
    }

}
