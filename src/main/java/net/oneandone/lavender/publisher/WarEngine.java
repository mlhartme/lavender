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

import net.oneandone.lavender.config.View;
import net.oneandone.lavender.filter.Lavender;
import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.PustefixModule;
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
 * Drives the war publishing process: Extracts resources from a war files to distributors and creates a new war file with an index and a
 * nodes files.
 */
public class WarEngine {
    private static final Logger LOG = LoggerFactory.getLogger(WarEngine.class);

    private final View view;
    private final String indexName;
    private final String svnUsername;
    private final String svnPassword;
    private final FileNode inputWar;
    private final FileNode outputWar;
    /** maps type to distributor */
    public final Map<String, Distributor> distributors;
    private final Index outputIndex;
    private final FileNode outputNodesFile;
    private final String nodes;

    public WarEngine(View view, String indexName, String svnUsername, String svnPassword,
                     FileNode inputWar, FileNode outputWar, Index outputIndex, FileNode outputNodesFile, String nodes) {
        this.view = view;
        this.indexName = indexName;
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.inputWar = inputWar;
        this.outputWar = outputWar;
        this.distributors = new HashMap<>();
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
        List<Module> modules;
        Index index;
        long changed;

        started = System.currentTimeMillis();
        modules = PustefixModule.fromWebapp(inputWar.openZip(), svnUsername, svnPassword);
        changed = extract(modules);
        for (Map.Entry<String, Distributor> entry : distributors.entrySet()) {
            index = entry.getValue().close();
            //  TODO
            if (View.WEB.equals(entry.getKey()) && index != null /* for tests */) {
                for (Label label : index) {
                    outputIndex.add(label);
                }
            }
        }
        outputNodesFile.writeString(nodes);
        updateWarFile();
        LOG.info("done: " + changed + "/" + outputIndex.size() + " files changed (" + (System.currentTimeMillis() - started) + " ms)");
    }

    public long extract(Module... modules) throws IOException {
        return extract(Arrays.asList(modules));
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
                distributor = view.get(type).open(inputWar.getWorld(), indexName);
                distributors.put(type, distributor);
            }
            changed += module.run(distributor);
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

        ZipEntry indexEntry = new ZipEntry(Lavender.LAVENDEL_IDX);
        out.putNextEntry(indexEntry);
        outputIndex.save(out);
        out.closeEntry();

        ZipEntry nodesEntry = new ZipEntry(Lavender.LAVENDEL_NODES);
        out.putNextEntry(nodesEntry);
        outputNodesFile.writeTo(out);
        out.closeEntry();

        out.close();
    }
}
