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

import net.oneandone.lavender.filter.Lavender;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.publisher.pustefix.PustefixSource;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Drives the war publishing process: Extracts resources from a war files to FileStorage and creates a new war file with Lavendelizer
 * configured. Main class of this module, used by Cli and the Publisher plugin.
 */
public class WarEngine {
    private static final Logger LOG = LoggerFactory.getLogger(WarEngine.class);

    private final String svnUsername;
    private final String svnPassword;
    private final FileNode inputWar;
    private final FileNode outputWar;
    private final Map<String, Distributor> storages;
    private final Index outputIndex;
    private final FileNode outputNodesFile;
    private final String nodes;

    public WarEngine(String svnUsername, String svnPassword,
                     FileNode inputWar, FileNode outputWar, Distributor lavendelStorage,
                     Index outputIndex, FileNode outputNodesFile, String nodes) {
        this(svnUsername, svnPassword, inputWar, outputWar, defaultStorage(lavendelStorage), outputIndex, outputNodesFile, nodes);
    }

    /**
     * @param inputWar
     *            the existing original WAR file
     * @param outputWar
     *            the file where the updated WAR file is saved to
     * @param storages
     * @param outputIndex
     *            Index of the resources for this war
     * @param outputNodesFile
     *            the file where the nodes file is saved to
     * @param nodes
     *            the lavender nodes. Each URI must contain the scheme (http or https), hostname, optional port, and
     *            optional path. The collection must contain separate URIs for http and https.
     */
    public WarEngine(String svnUsername, String svnPassword,
                     FileNode inputWar, FileNode outputWar, Map<String, Distributor> storages,
                     Index outputIndex, FileNode outputNodesFile, String nodes) {
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.inputWar = inputWar;
        this.outputWar = outputWar;
        this.storages = storages;
        this.outputIndex = outputIndex;
        this.outputNodesFile = outputNodesFile;
        this.nodes = nodes;
    }

    /**
     * Lavendelizes the WAR file and publishes resources.
     *
     * @throws IOException
     */
    public void run() throws IOException {
        long started;
        List<Source> sources;
        Index index;
        long changed;

        started = System.currentTimeMillis();
        sources = PustefixSource.fromWebapp(inputWar.openZip(), svnUsername, svnPassword);
        changed = extract(sources);
        for (Map.Entry<String, Distributor> entry : storages.entrySet()) {
            index = entry.getValue().close();
            //  TODO
            if (!entry.getKey().contains("flash") && index != null /* for tests */) {
                for (Label label : index) {
                    outputIndex.add(label);
                }
            }
        }
        outputNodesFile.writeString(nodes);
        updateWarFile();
        LOG.info("done: " + changed + "/" + outputIndex.size() + " files changed (" + (System.currentTimeMillis() - started) + " ms)");
    }

    public long extract(Source... sources) throws IOException {
        return extract(Arrays.asList(sources));
    }

    public long extract(List<Source> sources) throws IOException {
        Distributor storage;
        long changed;

        changed = 0;
        for (Source source : sources) {
            storage = storages.get(source.getStorage());
            if (storage == null) {
                throw new IllegalStateException("storage not found: " + source.getStorage());
            }
            changed += source.run(storage);
        }
        return changed;
    }

    private void updateWarFile() throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(inputWar.toPath().toFile()));
        ZipOutputStream out = new ZipOutputStream(outputWar.createOutputStream());
        ZipEntry entry;
        Buffer buffer;
        String name;

        entry = zin.getNextEntry();
        buffer = outputWar.getWorld().getBuffer();
        while (entry != null) {
            name = entry.getName();
            ZipEntry outEntry = new ZipEntry(name);
            out.putNextEntry(outEntry);
            buffer.copy(zin, out);
            out.closeEntry();
            entry = zin.getNextEntry();
        }
        zin.close();

        ZipEntry indexEntry = new ZipEntry(Lavender.LAVENDEL_IDX.substring(1));
        out.putNextEntry(indexEntry);
        outputIndex.save(out);
        out.closeEntry();

        ZipEntry nodesEntry = new ZipEntry(Lavender.LAVENDEL_NODES.substring(1));
        out.putNextEntry(nodesEntry);
        outputNodesFile.writeTo(out);
        out.closeEntry();

        out.close();
    }

    //--

    private static Map<String, Distributor> defaultStorage(Distributor lavendelStorage) {
        Map<String, Distributor> storages;

        storages = new HashMap<>();
        storages.put(Source.DEFAULT_STORAGE, lavendelStorage);
        return storages;
    }
}
