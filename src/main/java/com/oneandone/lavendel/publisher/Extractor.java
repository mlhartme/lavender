package com.oneandone.lavendel.publisher;

import com.oneandone.lavendel.index.Label;
import com.oneandone.lavendel.publisher.config.Filter;
import com.oneandone.lavendel.publisher.pustefix.PustefixExtractor;
import com.oneandone.lavendel.publisher.svn.SvnExtractorConfig;
import net.oneandone.sushi.fs.file.FileNode;

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
public abstract class Extractor implements Iterable<Resource> {
    public static final String DEFAULT_STORAGE = "lavendel";

    private static final String PROPERTIES = "WEB-INF/lavendel.properties";

    public static List<Extractor> fromWar(Log log, FileNode war) throws IOException {
        List<Extractor> result;
        Properties properties;

        log.verbose("scanning " + war);
        result = new ArrayList<>();
        properties = getConfig(war.toPath().toFile());
        result.add(PustefixExtractor.forProperties(war.toPath().toFile(), properties));
        for (SvnExtractorConfig config : SvnExtractorConfig.parse(properties)) {
            log.info("adding svn extractor " + config.name);
            result.add(config.create(war.getWorld(), log));
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
            throw new FileNotFoundException("missing " + PROPERTIES);
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

    public Extractor(Filter filter, String storage, boolean lavendelize, String pathPrefix) {
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
        boolean changed;
        long count;

        count = 0;
        config = getFilter();
        for (Resource resource : this) {
            if (config.isIncluded(resource.getPath())) {
                if (lavendelize) {
                    label = resource.labelLavendelized(pathPrefix);
                } else {
                    label = resource.labelNormal(pathPrefix);
                }
                if (distributor.write(label, resource.getData())) {
                    count++;
                }
            }
        }
        return count;
    }
}
