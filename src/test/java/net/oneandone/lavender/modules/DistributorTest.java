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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.index.Index;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Predicate;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DistributorTest {
    @Test
    public void write() throws IOException {
        World world;
        FileNode dir;
        Module<?> module;
        Index index;
        Distributor distributor;

        world = World.createMinimal();
        dir = world.guessProjectHome(getClass()).join("src/test/module");
        module = new NodeModule(Module.TYPE, "foo", true, "", "", world.filter().includeAll()) {
            @Override
            protected Map<String, Node> loadEntries() throws Exception {
                Map<String, Node> result;

                result = new HashMap<>();
                for (Node node : dir.find(getFilter().predicate(Predicate.FILE))) {
                    result.put(node.getRelative(dir), node);
                }
                return result;
            }
        };
        distributor = new Distributor(world.getTemp().createTempDirectory(), new HashMap<>(), new Index(), new Index());
        distributor.publish(module);
        index = distributor.close();
        assertEquals("264/5cfe2cb0a569e7d3daa64ebb35e26/foo/vi_login_now.jpg", index.lookup("vi_login_now.jpg").getLavendelizedPath());
    }
}
