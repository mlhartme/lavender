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
package net.oneandone.lavendel.publisher;

import net.oneandone.lavendel.filter.Lavendelizer;
import net.oneandone.lavendel.index.Index;
import net.oneandone.lavendel.publisher.config.Vhost;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class PublisherTest {
    @Test
    public void test() throws Exception {
        World world;
        FileNode tmp;

        world = new World();
        tmp = world.getTemp().createTempDirectory();
        FileNode outputDir = tmp.join("outputDir").mkdir();
        FileNode inputWar = (FileNode) world.resource("dummy.war");
        FileNode outputWar = tmp.join("outputWar");
        FileNode outputWebXmlFile = tmp.join("outputWebXmlFile").mkfile();
        Index outputIndex = new Index();
        FileNode outputNodesFile = tmp.join("outputNodesFile").mkfile();
        Distributor distributor = Distributor.forTest(outputDir, "notused");
        WarEngine extractor = new WarEngine(WarEngine.createNullLog(), inputWar, outputWar,
                distributor, outputWebXmlFile, outputIndex, outputNodesFile, Vhost.one("a.b.c").nodesFile());
        extractor.run();
        assertTrue(outputWar.exists());

        List<String> webXmlContent = getLines(outputWar, "WEB-INF/web.xml");
        assertTrue(webXmlContent.contains("  <filter-class>net.oneandone.lavendel.filter.Lavendelizer</filter-class>"));

        List<String> lavendelIdxContent = getLines(outputWar, Lavendelizer.LAVENDEL_IDX.substring(1));
        assertTrue(lavendelIdxContent.toString(), lavendelIdxContent
                .contains("modules/frontend-tools/img/delete.png=684/61ca5187cd2c6af08786467085f2b/frontend-tools"
                        + "/delete.png\\:68461ca5187cd2c6af08786467085f2b"));

        List<String> lavendelNodesContent = getLines(outputWar, Lavendelizer.LAVENDEL_NODES.substring(1));
        assertTrue(lavendelNodesContent.contains("http://a.b.c"));
        assertTrue(lavendelNodesContent.contains("https://a.b.c"));
        File extractedFile = new File(outputDir.toPath().toFile(), "684/61ca5187cd2c6af08786467085f2b/frontend-tools/delete.png");
        assertTrue(extractedFile.exists());
    }

    private List<String> getLines(FileNode outputWar, String path) throws IOException {
        return outputWar.openJar().join(path).readLines();
    }
}
