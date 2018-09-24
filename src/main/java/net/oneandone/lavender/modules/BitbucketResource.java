package net.oneandone.lavender.modules;

import java.io.IOException;
import java.io.OutputStream;

public class BitbucketResource extends Resource {
    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String resourcePath;
    private final String accessPath;
    private final String at;
    private final String contentId;

    public BitbucketResource(Bitbucket bitbucket, String project, String repository, String resourcePath, String accessPath, String at, String contentId) {
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.resourcePath = resourcePath;
        this.accessPath = accessPath;
        this.at = at;
        this.contentId = contentId;
    }

    @Override
    public String getPath() {
        return resourcePath;
    }

    @Override
    public String getContentId() {
        return contentId;
    }

    @Override
    public String getOrigin() {
        return "bitbucket:" + project + ":" + repository + ":" + accessPath + ":" + at;
    }

    @Override
    public void writeTo(OutputStream dest) throws IOException {
        bitbucket.writeTo(project, repository, accessPath, at, dest);
    }

    @Override
    public boolean isOutdated() {
        return false; // TODO
    }
}
