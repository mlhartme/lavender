package com.oneandone.lavendel.publisher.svn;

import com.oneandone.lavendel.publisher.Extractor;
import com.oneandone.lavendel.publisher.Resource;
import com.oneandone.lavendel.publisher.config.Filter;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** Extracts resources from svn */
public class SvnExtractor extends Extractor {
    private final List<Node> resources;
    private final String name;
    private final Node dest;

    public SvnExtractor(Filter filter, String storage, boolean lavendelize, String pathPrefix,
                        List<Node> resources, String name, Node dest) {
        super(filter, storage, lavendelize, pathPrefix);
        this.resources = resources;
        this.name = name;
        this.dest = dest;
    }

    public Iterator<Resource> iterator() {
        final Iterator<Node> base;

        base = resources.iterator();
        return new Iterator<Resource>() {
            public boolean hasNext() {
                return base.hasNext();
            }

            public Resource next() {
                Node file;

                file = base.next();
                try {
                    return new Resource(file.readBytes(), file.getRelative(dest), name);
                } catch (IOException e) {
                    throw new RuntimeException("TODO", e);
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
