package net.oneandone.lavender.scm;

import java.io.IOException;
import java.io.OutputStream;

public class BitbucketScmRoot extends ScmRoot {
    private final Bitbucket bitbucket;
    private final String project;
    private final String repository;
    private final String at;

    public BitbucketScmRoot(Bitbucket bitbucket, String project, String repository, String at) {
        this.bitbucket = bitbucket;
        this.project = project;
        this.repository = repository;
        this.at = at;
    }

    public String getOrigin() {
        return bitbucket.getOrigin(project, repository);
    }

    public void writeTo(String path, OutputStream dest) throws IOException {
        bitbucket.writeTo(project, repository, path, at, dest);
    }
}
