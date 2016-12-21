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

import com.sun.nio.zipfs.ZipFileSystemProvider;
import com.sun.nio.zipfs.ZipPath;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.filter.Lavender;
import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.DefaultModule;
import net.oneandone.lavender.modules.Module;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    /** maps type to Distributor */
    private final Map<String, Distributor> distributors;
    private final String indexName;
    private final String svnUsername;
    private final String svnPassword;
    private final FileNode war;
    private final FileNode outputNodesFile;
    private final String nodes;

    public WarEngine(FileNode cache, Map<String, Distributor> distributors, String indexName, String svnUsername, String svnPassword,
                     FileNode war, FileNode outputNodesFile, String nodes) {
        this.cache = cache;
        this.distributors = distributors;
        this.indexName = indexName;
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.war = war;
        this.outputNodesFile = outputNodesFile;
        this.nodes = nodes;
    }

    /**
     * Lavendelizes the WAR file and publishes resources.
     *
     * @return types mapped to indexes
     */
    public Map<String, Index> run() throws IOException, XmlException, SAXException {
        long started;
        List<Module> modules;
        Index index;
        long absolute;
        long changed;
        Map<String, Index> result;
        long warStart;

        started = System.currentTimeMillis();
        modules = DefaultModule.fromWebapp(cache, true, war.openZip(), svnUsername, svnPassword);
        absolute = 0;
        changed = extract(modules);
        result = new HashMap<>();
        for (Map.Entry<String, Distributor> entry : distributors.entrySet()) {
            index =  entry.getValue().close();
            absolute += index.size();
            result.put(entry.getKey(), index);
        }
        LOG.info("lavender servers updated: "
                + changed + "/" + absolute + " files changed in " + modules.size() + " modules, " + (System.currentTimeMillis() - started) + " ms");
        outputNodesFile.writeString(nodes);
        warStart = System.currentTimeMillis();
        updateWarFile(result.get(Docroot.WEB), outputNodesFile);
        LOG.info("updated war " + (war.length() / 1024) + "k, " + (System.currentTimeMillis() - warStart) + " ms");
        for (Module module : modules) {
            module.saveCaches();
        }
        return result;
    }

    public long extract(List<Module> modules) throws IOException {
        Distributor distributor;
        String type;
        long changed;

        changed = 0;
        for (Module module : modules) {
            type = module.getType();
            distributor = distributors.get(type);
            if (distributor == null) {
                // nothing to do - this type is not published
            } else {
                changed += module.publish(distributor);
            }
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
        ZipFileSystemProvider provider = new ZipFileSystemProvider();
        Map<String, Object> env = new HashMap<>();

        try (FileSystem fs = provider.newFileSystem(war.toPath(), env)) {
            ZipPath entry = (ZipPath) fs.getPath(Lavender.LAVENDER_IDX);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            webIndex.save(output);
            Files.copy(new ByteArrayInputStream(output.toByteArray()), entry, StandardCopyOption.REPLACE_EXISTING);

            entry = (ZipPath) fs.getPath(Lavender.LAVENDER_NODES);
            Files.copy(nodesFile.createInputStream(), entry, StandardCopyOption.REPLACE_EXISTING);
        };
    }

}
