package com.oneandone.lavendel.publisher.pustefix;

import com.oneandone.lavendel.publisher.Extractor;
import com.oneandone.lavendel.publisher.Resource;
import com.oneandone.lavendel.publisher.config.Filter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Extracts static resources from a Pustefix application. Valid static resource path are defined in WEB-INF/project.xml.
 * Resources can be found in the WAR or in nested JARs.
 */
public class PustefixExtractor extends Extractor {
    private static final List<String> DEFAULT_INCLUDE_EXTENSIONS = new ArrayList<>(Arrays.asList(
            "gif", "png", "jpg", "jpeg", "ico", "swf", "css", "js"));

    public static PustefixExtractor forProperties(File war, Properties properties) {
        return new PustefixExtractor(Filter.forProperties(properties, "pustefix", DEFAULT_INCLUDE_EXTENSIONS), war);
    }

    private final File war;

    public PustefixExtractor(Filter filter, File war) {
        super(filter, DEFAULT_STORAGE, true, "");
        this.war = war;
    }

    public Iterator<Resource> iterator() {
        return new PustefixResourceIterator(war);
    }
}
