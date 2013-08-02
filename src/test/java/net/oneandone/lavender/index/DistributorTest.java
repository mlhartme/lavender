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
package net.oneandone.lavender.index;

import net.oneandone.lavender.modules.DefaultResource;
import net.oneandone.lavender.modules.Resource;
import net.oneandone.sushi.fs.Node;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class DistributorTest {

    @Test
    public void write() throws IOException {
        Index index;

        Resource resource1 = DefaultResource.forBytes("abcd".getBytes(), "img/test.png");
        Resource resource2 = DefaultResource.forBytes("abcd".getBytes(), "modules/stageassistent/img/test.gif");
        Distributor distributor = new Distributor(new HashMap<Node, Node>(), new Index(), new Index());
        distributor.write(resource1.labelLavendelized("", "folder"), resource1);
        distributor.write(resource2.labelLavendelized("", "stageassistent"), resource1);
        index = distributor.close();
        assertEquals("e2f/c714c4727ee9395f324cd2e7f331f/folder/test.png", index.lookup("img/test.png").getLavendelizedPath());
        assertEquals("e2f/c714c4727ee9395f324cd2e7f331f/stageassistent/test.gif",
                index.lookup("modules/stageassistent/img/test.gif").getLavendelizedPath());
    }

}
