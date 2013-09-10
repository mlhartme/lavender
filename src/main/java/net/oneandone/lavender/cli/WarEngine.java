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
package net.oneandone.lavender.cli;

import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.filter.Lavender;
import net.oneandone.lavender.index.Distributor;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.PustefixModule;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
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

    /** maps type to Distributor */
    private final Map<String, Distributor> distributors;
    private final String indexName;
    private final String svnUsername;
    private final String svnPassword;
    private final FileNode inputWar;
    private final FileNode outputWar;
    private final FileNode outputNodesFile;
    private final String nodes;

    public WarEngine(Map<String, Distributor> distributors, String indexName, String svnUsername, String svnPassword,
                     FileNode inputWar, FileNode outputWar, FileNode outputNodesFile, String nodes) {
        this.distributors = distributors;
        this.indexName = indexName;
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.inputWar = inputWar;
        this.outputWar = outputWar;
        this.outputNodesFile = outputNodesFile;
        this.nodes = nodes;
    }

    /**
     * Lavendelizes the WAR file and publishes resources.
     *
     * @return types mapped to indexes
     */
    public Map<String, Index> run() throws IOException {
        long started;
        List<Module> modules;
        Index index;
        long absolute;
        long changed;
        Map<String, Index> result;

        started = System.currentTimeMillis();
        modules = PustefixModule.fromWebapp(inputWar.openZip(), svnUsername, svnPassword);
        absolute = 0;
        changed = extract(modules);
        result = new HashMap<>();
        for (Map.Entry<String, Distributor> entry : distributors.entrySet()) {
            index =  entry.getValue().close();
            absolute += index.size();
            result.put(entry.getKey(), index);
        }
        LOG.info("lavendel servers updated: "
                + changed + "/" + absolute + " files changed (" + (System.currentTimeMillis() - started) + " ms)");
        outputNodesFile.writeString(nodes);
        updateWarFile(result.get(Docroot.WEB));
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

    private void updateWarFile(Index webIndex) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(inputWar.toPath().toFile()));
        ZipOutputStream out = new ZipOutputStream(outputWar.createOutputStream());
        Buffer buffer;
        String name;

        buffer = outputWar.getWorld().getBuffer();
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            name = entry.getName();
            ZipEntry outEntry = new ZipEntry(name);
            out.putNextEntry(outEntry);
            buffer.copy(zin, out);
            out.closeEntry();
        }
        zin.close();

        ZipEntry indexEntry = new ZipEntry(Lavender.LAVENDEL_IDX);
        out.putNextEntry(indexEntry);
        webIndex.save(out);
        out.closeEntry();

        ZipEntry nodesEntry = new ZipEntry(Lavender.LAVENDEL_NODES);
        out.putNextEntry(nodesEntry);
        outputNodesFile.writeTo(out);
        out.closeEntry();

        out.close();
    }
}
