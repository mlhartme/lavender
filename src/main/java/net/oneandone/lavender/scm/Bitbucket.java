/*
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
package net.oneandone.lavender.scm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpFilesystem;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.io.Buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bitbucket rest api. As of 2018-09-18, we have bitbucket server 5.13.1. As of 2019-12-10 we have 6.8.0
 * <a href="https://docs.atlassian.com/bitbucket-server/rest/6.8.0/bitbucket-rest.html">Rest API documentation</a>
 * Note that there are also Cloud APIs (https://developer.atlassian.com/bitbucket/api/2/reference/resource/), but they
 * are not available on our server.
 */
public class Bitbucket {
    public static Bitbucket create(World world, String hostname, UsernamePassword up) throws NodeInstantiationException {
        String credentials;
        String wireLog;
        URI uri;

        wireLog = System.getProperty("lavender.wirelog");
        if (wireLog != null) {
            HttpFilesystem.wireLog(wireLog);
        }

        if (up != null && !up.equals(UsernamePassword.ANONYMOUS)) {
            credentials = up.username + ":" + up.password;
        } else {
            credentials = null;
        }
        try {
            uri = new URI("https", credentials, hostname, -1, "/rest/api/1.0", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return new Bitbucket((HttpNode) world.node(uri));
    }


    // https://stackoverflow.com/questions/9765453/is-gits-semi-secret-empty-tree-object-reliable-and-why-is-there-not-a-symbolic
    private static final String NULL_COMMIT = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    public static void main(String[] args) throws IOException {
        run("CISOOPS", "puc");
    }

    public static void run(String project, String repository) throws IOException {
        World world;
        Bitbucket bitbucket;
        List<String> files;
        List<String> directories;
        String latestCommit;
        Map<String, String> contentMap;

        world = World.create(false);
        bitbucket = new Bitbucket((HttpNode) world.validNode("https://bitbucket.1and1.org/rest/api/1.0"));
        latestCommit = bitbucket.latestCommit(project, repository, "master");
        files = bitbucket.files(project, repository, latestCommit);
        directories = directories(files);
        System.out.println("files: " + files.size() + " " + files);
        System.out.println("directories: " + directories.size() + " " + directories);
        contentMap = new HashMap<>();
        for (String d : directories) {
            bitbucket.lastModified(project, repository, d, latestCommit, contentMap);
        }
        System.out.println("contentMap: " + contentMap.size() + " " + contentMap);
    }

    private static List<String> directories(List<String> files) {
        List<String> directories;
        int idx;
        String directory;

        directories = new ArrayList<>();
        for (String file : files) {
            idx = file.lastIndexOf('/');
            directory = idx == -1 ? "" : file.substring(0, idx);
            if (!directories.contains(directory)) {
                directories.add(directory);
            }
        }
        return directories;
    }

    private final HttpNode api;
    private final JsonParser parser;

    public Bitbucket(HttpNode api) {
        this.api = api;
        this.parser = new JsonParser();
    }

    public String getOrigin(String project, String repository) {
        return api.getRoot().node("", null).join(project, repository).getUri().toString();
    }

    public String latestCommit(String project, String repository, String branchOrTag) throws IOException {
        List<String> result;

        result = new ArrayList<>();
        getPaged(element -> {
            JsonObject obj;

            obj = element.getAsJsonObject();
            if (branchOrTag.equals(obj.get("displayId").getAsString())) {
                result.add(obj.get("latestCommit").getAsString());
            }
        }, api.join("projects", project, "repos", repository, "branches"));
        switch (result.size()) {
            case 0: break; // fall-through
            case 1: return result.get(0);
            default: throw new IOException(branchOrTag + ": branch ambiguous: " + result);
        }

        getPaged(element -> {
            JsonObject obj;

            obj = element.getAsJsonObject();
            if (branchOrTag.equals(obj.get("displayId").getAsString())) {
                result.add(obj.get("latestCommit").getAsString());
            }
        }, api.join("projects", project, "repos", repository, "tags"));
        switch (result.size()) {
            case 0: return null;
            case 1:
                return result.get(0);
            default: throw new IOException(branchOrTag + ": tag ambiguous: " + result);
        }
    }

    public String lastModified(String project, String repository, String directory, String at,
                                 Map<String, String> result) throws IOException {
        HttpNode req;
        JsonObject response;

        req = api.join("projects", project, "repos", repository, "last-modified/" + directory).withParameter("at", at);
        response = parser.parse(req.readString()).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : response.get("files").getAsJsonObject().entrySet()) {
            String name;
            String path;

            name = entry.getKey();
            path = directory.isEmpty() ? name : directory + "/" + name;
            result.put(path, entry.getValue().getAsJsonObject().get("id").getAsString());
        }
        return response.get("latestCommit").getAsJsonObject().get("id").getAsString();
    }

    /**
     * Does not return removals! CAUTION: Bitbucket returns 1000 max entries, even though I use paging,
     * which makes this method useless for real-world apps.
     *
     * @param from commit that contains the requested changes
     * @param to   what to compare to, in my case the last revision I've seen - or the null revision
     * @return path- to contentId mapping
     */
    public Map<String, String> changes(String project, String repository, String from, String to) throws IOException {
        Map<String, String> result;

        result = new HashMap<>();
        getPaged(element -> {
            JsonObject obj;
            JsonObject path;
            String parent;

            obj = element.getAsJsonObject();
            path = obj.get("path").getAsJsonObject();
            parent = path.get("parent").getAsString();
            if (!parent.isEmpty()) {
                parent = parent + "/";
            }
            result.put(parent + path.get("name").getAsString(), obj.get("contentId").getAsString());
        }, api.join("projects", project, "repos", repository, "compare/changes"), "from", from, "to", to);
        return result;
    }

    public Map<String, String> changes(String project, String repository, String from) throws IOException {
        return changes(project, repository, from, NULL_COMMIT);
    }

    /**
     * List files in the specified project + revision
     *
     * @param at revision
     */
    // curl 'https://bitbucket.1and1.org/rest/api/1.0/projects/CISOOPS/repos/lavender-test-module/files'  | python -m json.tool
    public List<String> files(String project, String repository, String at) throws IOException {
        List<String> result;

        result = new ArrayList<>();
        getPaged(element -> result.add(element.getAsString()), api.join("projects", project, "repos", repository, "files"), "at", at);
        return result;
    }

    private static final String UTF_8 = "UTF-8";
    private static final HeaderList LFS_HEADERS = HeaderList.of(
            "Accept", "application/vnd.git-lfs+json",
            "Content-Type", "application/vnd.git-lfs+json");
    private static final byte[] LFS_IDENTIFIER;
    static {
        try {
            LFS_IDENTIFIER = "version https://git-lfs.github.com/spec/v1\n".getBytes(UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTo(String project, String repository, String path, String at, OutputStream dest) throws IOException {
        HttpNode node;
        Buffer buffer;
        int bytesRead;

        node = api.join("projects", project, "repos", repository, "raw", path);
        node = node.withParameter("at", at);
        buffer = node.getWorld().getBuffer();
        try (InputStream from = node.newInputStream()) {
            bytesRead = buffer.fill(from, LFS_IDENTIFIER.length);
            if (buffer.diff(LFS_IDENTIFIER, bytesRead)) {
                // regular file
                buffer.flush(dest, bytesRead);
                buffer.copy(from, dest);
            } else {
                lfsWriteTo(buffer.readString(from, UTF_8), project, repository, dest);
            }
        }
    }

    // https://github.com/git-lfs/git-lfs/blob/master/docs/api/batch.md
    private void lfsWriteTo(String content, String project, String repository, OutputStream dest) throws IOException {
        int idx;
        String oidLine;
        String sizeLine;
        String oid;
        long size;

        idx = content.indexOf('\n');
        oidLine = content.substring(0, idx).trim();
        sizeLine = content.substring(idx + 1).trim();
        if (oidLine.startsWith("oid sha256:")) {
            oid = oidLine.substring(11);
        } else {
            throw new RuntimeException("LFS link does not contain supported oid: " + oidLine);
        }
        if (sizeLine.startsWith("size ")) {
            size = Long.parseLong(sizeLine.substring(5));
        } else {
            throw new RuntimeException("LFS link does not contain supported size: " + sizeLine);
        }
        lfsWriteTo(oid, size, project, repository, dest);
    }

    private void lfsWriteTo(String oid, long size, String project, String repository, OutputStream dest) throws IOException {
        HttpNode lfs;
        JsonElement response;
        JsonArray array;
        String url;

        lfs = api.getRootNode().join("scm", project, repository + ".git", "info/lfs/objects/batch").withHeaders(LFS_HEADERS);
        response = parser.parse(lfs.post(String.format(
                "{\"operation\": \"download\", \"transfers\": [\"basic\"], \"objects\": [{\"oid\": \"%s\", \"size\": %d}]}", oid, size)));
        array = response.getAsJsonObject().get("objects").getAsJsonArray();
        if (array.size() != 1) {
            throw new RuntimeException("Unique object for LFS link not found: " + response);
        }
        url = array.get(0).getAsJsonObject().get("actions").getAsJsonObject().get("download").getAsJsonObject().get("href").getAsString();
        lfs.getWorld().validNode(url).copyFileTo(dest);
    }

    private interface Collector {
        void add(JsonElement element);
    }

    public void getPaged(Collector collector, HttpNode node, String... params) throws IOException {
        HttpNode req;
        int start;
        JsonObject response;
        JsonArray values;
        JsonElement next;

        start = 0;
        while (true) {
            req = node.withParameters("start", Integer.toString(start), "limit", "100").withParameters(params);
            response = parser.parse(req.readString()).getAsJsonObject();
            values = response.get("values").getAsJsonArray();
            for (JsonElement element : values) {
                collector.add(element);
            }
            if (response.get("isLastPage").getAsBoolean()) {
                break;
            }
            next = response.get("nextPageStart");
            if (next.isJsonNull()) {
                throw new IOException("TODO: this seems to indicate we've hit a hard server limit: "
                        + "there are more entries, but it won't return them ...");
            }
            start = next.getAsInt();
        }
    }
}
