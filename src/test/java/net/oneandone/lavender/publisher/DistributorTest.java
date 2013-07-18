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
package net.oneandone.lavender.publisher;

import net.oneandone.lavender.index.Index;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class DistributorTest {

    @Test
    public void write() throws IOException {
        Index index;
        World world;
        Node node;

        world = new World();
        node = world.memoryNode("abcd");
        Resource resource1 = new Resource(node, "img/test.png", "folder");
        Resource resource2 = new Resource(node, "modules/stageassistent/img/test.gif", "stageassistent");
        Distributor storage = new Distributor(new HashMap<Node, Node>(), new Index());
        storage.write(resource1.labelLavendelized("", node.readBytes()), resource1.readData());
        storage.write(resource2.labelLavendelized("", node.readBytes()), resource1.readData());
        index = storage.close();
        assertEquals("e2f/c714c4727ee9395f324cd2e7f331f/folder/test.png", index.lookup("img/test.png").getLavendelizedPath());
        assertEquals("e2f/c714c4727ee9395f324cd2e7f331f/stageassistent/test.gif",
                index.lookup("modules/stageassistent/img/test.gif").getLavendelizedPath());
    }

}
