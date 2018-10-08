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
package net.oneandone.lavender.cli;

import net.oneandone.lavender.config.Secrets;
import net.oneandone.lavender.filter.Lavender;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Distributor;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.NodeModule;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives the war publishing process: Extracts resources from a war files to distributors and creates a new war file with an index and a
 * nodes files.
 */
public class WarEngine {
    private static final Logger LOG = LoggerFactory.getLogger(WarEngine.class);

    private final FileNode cache;
    private final Distributor distributor;
    private final Secrets secrets;
    private final FileNode war;
    private final FileNode outputNodesFile;
    private final String nodes;

    public WarEngine(FileNode cache, Distributor distributor, Secrets secrets, FileNode war, FileNode outputNodesFile, String nodes) {
        this.cache = cache;
        this.distributor = distributor;
        this.secrets = secrets;
        this.war = war;
        this.outputNodesFile = outputNodesFile;
        this.nodes = nodes;
    }

    /**
     * Lavendelizes the WAR file and publishes resources.
     *
     * @return types mapped to indexes
     */
    public Index run() throws IOException {
        long started;
        List<Module> modules;
        Index index;
        long absolute;
        long changed;
        long warStart;

        started = System.currentTimeMillis();
        modules = NodeModule.fromWebapp(cache, true, war.openZip(), secrets);
        absolute = 0;
        changed = extract(modules);

        index =  distributor.close();
        absolute += index.size();

        outputNodesFile.writeString(nodes);
        warStart = System.currentTimeMillis();
        updateWarFile(index, outputNodesFile);
        LOG.info("updated war " + (war.size() / 1024) + "k, " + (System.currentTimeMillis() - warStart) + " ms");
        LOG.info("done: "
                + changed + "/" + absolute + " files changed in " + modules.size() + " modules, " + (System.currentTimeMillis() - started) + " ms");
        return index;
    }

    private long extract(List<Module> modules) throws IOException {
        long changed;

        changed = 0;
        for (Module module : modules) {
            changed += distributor.publish(module);
        }
        return changed;
    }

    /**
     * Add lavender.idx and lavender.nodes to war file using ZipFileSystemProvider. It assumes that the
     * WEB-INF directory already in the war file
     *
     * @param webIndex Lavender index for lavender.idx file containing mappings from originalPath to
     *                 lavenderized path (CDN paths)
     * @param nodesFile Nodes for lavender.nodes file containing all CDN hosts
     * @throws IOException
     */
    private void updateWarFile(Index webIndex, Node nodesFile) throws IOException {
        Map<String, Object> env;
        Path entry;
        ByteArrayOutputStream output;

        env = new HashMap<>();
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + war.getUri().toString()), env, null)) {
            entry = fs.getPath(Lavender.LAVENDER_IDX);
            output = new ByteArrayOutputStream();
            webIndex.save(output);
            Files.copy(new ByteArrayInputStream(output.toByteArray()), entry, StandardCopyOption.REPLACE_EXISTING);

            entry = fs.getPath(Lavender.LAVENDER_NODES);
            Files.copy(nodesFile.newInputStream(), entry, StandardCopyOption.REPLACE_EXISTING);
        };
    }

}
