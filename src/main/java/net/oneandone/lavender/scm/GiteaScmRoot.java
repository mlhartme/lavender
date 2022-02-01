package net.oneandone.lavender.scm;

import io.gitea.ApiClient;
import io.gitea.ApiException;
import io.gitea.Configuration;
import io.gitea.api.RepositoryApi;
import io.gitea.auth.ApiKeyAuth;
import io.gitea.model.ContentsResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GiteaScmRoot extends ScmRoot {
    private final RepositoryApi gitea;
    private final String organization;
    private final String repository;
    private final String ref;

    public GiteaScmRoot(String host, String organization, String repository, String ref) {
        ApiClient client;

        client = Configuration.getDefaultApiClient();
        client.setBasePath("https://" + host + "/api/v1");
        client.setReadTimeout(5000);

        /* TODO
        ApiKeyAuth accessToken = (ApiKeyAuth) defaultClient.getAuthentication("AccessToken");
        accessToken.setApiKey(apiKey); */

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

    public String read(String path) throws ApiException {
        ContentsResponse content = gitea.repoGetContents(organization, repository, path, ref);

        String str = content.getContent();
        if ("base64".equals(content.getEncoding())) {
            str = new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }
        return str;
    }
}
