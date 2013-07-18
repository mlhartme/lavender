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

import net.oneandone.lavender.index.Label;
import net.oneandone.lavender.publisher.config.Filter;
import net.oneandone.lavender.publisher.pustefix.PustefixSource;
import net.oneandone.lavender.publisher.svn.SvnSourceConfig;
import net.oneandone.sushi.fs.file.FileNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Extracts resources */
public abstract class Source implements Iterable<Resource> {
    private static final Logger LOG = LoggerFactory.getLogger(Source.class);

    public static final String DEFAULT_STORAGE = "lavender";

    private static final String PROPERTIES = "WEB-INF/lavender.properties";

    public static List<Source> fromWar(FileNode war, String svnUsername, String svnPassword) throws IOException {
        List<Source> result;
        Properties properties;

        LOG.trace("scanning " + war);
        result = new ArrayList<>();
        properties = getConfig(war.toPath().toFile());
        result.add(PustefixSource.forProperties(war.openZip(), properties));
        for (SvnSourceConfig config : SvnSourceConfig.parse(properties)) {
            LOG.info("adding svn source " + config.name);
            result.add(config.create(war.getWorld(), svnUsername, svnPassword));
        }
        return result;
    }

    private static Properties getConfig(File war) throws IOException {
        Properties result;
        ZipFile zip;
        ZipEntry entry;
        InputStream src;

        result = new Properties();
        zip = new ZipFile(war);
        entry = zip.getEntry(PROPERTIES);
        if (entry == null) {
            // TODO: dump this compatibility check as soon as I have ITs with new wars
            entry = zip.getEntry("WEB-INF/lavendel.properties");
            if (entry == null) {
                throw new FileNotFoundException("missing " + PROPERTIES);
            }
        }
        src = zip.getInputStream(entry);
        result.load(src);
        src.close();
        return result;
    }

    //--

    private final Filter filter;
    private final String storage;
    private final boolean lavendelize;
    private final String pathPrefix;

    public Source(Filter filter, String storage, boolean lavendelize, String pathPrefix) {
        if (filter == null) {
            throw new IllegalArgumentException();
        }
        this.filter = filter;
        this.storage = storage;
        this.lavendelize = lavendelize;
        this.pathPrefix = pathPrefix;
    }

    public String getStorage() {
        return storage;
    }

    public Filter getFilter() {
        return filter;
    }

    /** @return number of changed (updated or added) files */
    public long run(Distributor distributor) throws IOException {
        Filter config;
        Label label;
        long count;
        byte[] data;
        byte[] md5;

        count = 0;
        config = getFilter();
        for (Resource resource : this) {
            if (config.isIncluded(resource.getPath())) {
                data = resource.readData();
                md5 = Resource.md5(data);
                if (lavendelize) {
                    label = resource.labelLavendelized(pathPrefix, md5);
                } else {
                    label = resource.labelNormal(pathPrefix, md5);
                }
                if (distributor.write(label, data)) {
                    count++;
                }
            }
        }
        return count;
    }
}
