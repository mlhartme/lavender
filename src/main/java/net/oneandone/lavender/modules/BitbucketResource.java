package net.oneandone.lavender.modules;

import java.io.IOException;
import java.io.OutputStream;

public class BitbucketResource extends Resource {
    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String resourcePath;
    private final BitbucketEntry entry;
    private final String at;

    public BitbucketResource(Bitbucket bitbucket, String project, String repository, String resourcePath, BitbucketEntry entry, String at) {
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.resourcePath = resourcePath;
        this.entry = entry;
        this.at = at;
    }

    @Override
    public String getPath() {
        return resourcePath;
    }

    @Override
    public String getContentId() {
        return entry.contentId;
    }

    @Override
    public String getOrigin() {
        return "bitbucket:" + project + ":" + repository + ":" + entry + ":" + at;
    }

    @Override
    public void writeTo(OutputStream dest) throws IOException {
        bitbucket.writeTo(project, repository, entry.accessPath, at, dest);
    }

    @Override
    public boolean isOutdated() {
        // the least expensive way I know to check for changes is to re-load changes with content ids
        // (note that Bitbucket's lastModified api call didn't work for me, some command wasn'd found on the server)
        return true;
    }
}
