package net.oneandone.lavender.scm;

import io.gitea.ApiClient;
import io.gitea.ApiException;
import io.gitea.Configuration;
import io.gitea.api.RepositoryApi;
import io.gitea.auth.ApiKeyAuth;
import io.gitea.model.ContentsResponse;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Base64;

public class GiteaScmRoot extends ScmRoot {
    public static GiteaScmRoot create(URI uri, String at, String token) throws NodeInstantiationException {
        String uriPath;
        int idx;
        String project;
        String repository;

        uriPath = Strings.removeLeft(uri.getPath(), "/");
        idx = uriPath.indexOf('/');
        project = uriPath.substring(0, idx);
        repository = Strings.removeRight(uriPath.substring(idx + 1), ".git");
        return new GiteaScmRoot(uri.getHost(), project, repository, at, token);
    }

    private final RepositoryApi gitea;
    private final String organization;
    private final String repository;
    private final String ref;

    public GiteaScmRoot(String host, String organization, String repository, String ref, String token) {
        ApiClient client;

        client = Configuration.getDefaultApiClient();
        client.setBasePath("https://" + host + "/api/v1");
        client.setReadTimeout(5000);
        if (token != null) {
            ApiKeyAuth accessToken = (ApiKeyAuth) client.getAuthentication("AccessToken");
            accessToken.setApiKey(token);
        }

        this.gitea = new RepositoryApi(client);
        this.organization = organization;
        this.repository = repository;
        this.ref = ref;
    }

    public String getOrigin() {
        return gitea.getApiClient().getBasePath();
    }

    public void writeTo(String path, OutputStream dest) throws IOException {
    }

    public byte[] read(String path) throws ApiException, IOException {
        ContentsResponse content;

        content = gitea.repoGetContents(organization, repository, path, ref);
        switch (content.getEncoding()) {
            // TODO: more encodings?
            case "base64":
                return Base64.getDecoder().decode(content.getContent());
            default:
                throw new IOException("unknown encoding: " + content.getEncoding());
        }
    }
}
