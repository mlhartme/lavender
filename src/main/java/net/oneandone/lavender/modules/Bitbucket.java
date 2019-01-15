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
package net.oneandone.lavender.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.http.*;
import net.oneandone.sushi.fs.http.model.*;

import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Bitbucket rest api. As of 2018-09-18, we have bitbucket server 5.13.1.
 * <a href="https://docs.atlassian.com/bitbucket-server/rest/5.13.0/bitbucket-rest.html">Rest API documentation</a>
 */
public class Bitbucket {
    public static Bitbucket create(World world, String hostname, UsernamePassword up) throws NodeInstantiationException {
        String credentials;

        if (up != null) {
            credentials = up.username + ":" + up.password + "@";
        } else {
            credentials = "";
        }
        return new Bitbucket((HttpNode) world.validNode("https://" + credentials + hostname + "/rest/api/1.0"));
    }


    // https://stackoverflow.com/questions/9765453/is-gits-semi-secret-empty-tree-object-reliable-and-why-is-there-not-a-symbolic
    private static final String NULL_COMMIT = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        digests();
        run();
    }

    public static void digests() throws IOException, NoSuchAlgorithmException {
        World world;
        FileNode file;

        world = World.create();
        System.out.println("bitbucket content id: d9ffe8f740c9e629005a5c30de53c92e757dfb22");
        file = world.file("/Users/mhm/lavender-test-module/README");
        for (String name : new String[] { "md5", "sha", "sha-1", "sha-256", "sha-384", "sha-512"}) {
            System.out.println(name + ": " + file.digest(name));
        }
    }

    public static void run() throws IOException {
        World world;
        Bitbucket bitbucket;
        FileNode cache;

        String cachedCommit;
        String latestCommit;
        List<String> files;
        Map<String, String> changes;

        world = World.create(false);
        cache = world.file("cache");
        bitbucket = new Bitbucket((HttpNode) world.validNode("https://bitbucket.1and1.org/rest/api/1.0"));
        latestCommit = bitbucket.latestCommit("CISOOPS", "lavender-test-module", "master");
        if (cache.exists()) {
            System.out.println("no cache");
            files = cache.readLines();
            cachedCommit = files.remove(0);
            changes = bitbucket.changes("CISOOPS", "lavender-test-module", latestCommit, cachedCommit);
            System.out.println("changes: " + changes);
        } else {
            files = bitbucket.files("CISOOPS", "lavender-test-module", latestCommit);
            System.out.println("files: " + files);
            System.out.println("changes: " + bitbucket.changes("CISOOPS", "lavender-test-module", latestCommit, NULL_COMMIT));
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        bitbucket.writeTo("CISOOPS", "lavender-test-module", "README", latestCommit, buffer);
        System.out.println("file: " + buffer.toString());
    }

    private final HttpNode api;
    private final JsonParser parser;

    public Bitbucket(HttpNode root) {
        this.api = root;
        this.parser = new JsonParser();
    }

    public String getOrigin(String project, String repository) {
        return api.getRoot().node("", null).join(project, repository).getUri().toString();
    }

    public String latestCommit(String project, String repository, String branch) throws IOException {
        List<String> result;

        result = new ArrayList<>();
        getPaged(element -> {
            JsonObject obj;

            obj = element.getAsJsonObject();
            if (branch.equals(obj.get("displayId").getAsString())) {
                result.add(obj.get("latestCommit").getAsString());
            }
        }, api.join("projects", project, "repos", repository, "branches"));
        switch (result.size()) {
            case 0: return null;
            case 1: return result.get(0);
            default: throw new IOException(branch + ": branch ambiguous: " + result);
        }
    }

    /**
     * Does not return removals!
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

    public void writeTo(String project, String repository, String path, String at, OutputStream dest) throws IOException {
        HttpNode node;

//        https://github.com/git-lfs/git-lfs/blob/master/docs/api/batch.md

        node = api.join("projects", project, "repos", repository, "raw", path);
        node = node.getRoot().node(node.getPath(), "at=" + at);

        byte[] identifier = "version https://git-lfs.github.com/spec/v1\n".getBytes("UTF-8");
        InputStream from = node.newInputStream();
        byte[] buffer = new byte[8192];
        int bytesRead = from.read(buffer, 0, identifier.length);
        byte[] bufferSliceToCompare = Arrays.copyOfRange(buffer, 0, bytesRead);
        if (java.util.Arrays.equals(bufferSliceToCompare, identifier)) {
            // interpret as LFS link
            BufferedReader reader = new BufferedReader(new InputStreamReader(from));
            String oidLine = reader.readLine(); // read line with oid info
            String sizeLine = reader.readLine(); // read line with size info
            reader.close();
            String oid;
            if (oidLine.startsWith("oid sha256:")){
                oid = oidLine.substring(11);
            } else {
                throw new RuntimeException("LFS link does not contain supported oid.");
            }
            long size;
            if (sizeLine.startsWith("size ")){
                size = Long.parseLong(sizeLine.substring(5));
            } else {
                throw new RuntimeException("LFS link does not contain supported size.");
            }

            String lfsApiPath = String.join("/", "scm", project, repository + ".git", "info/lfs/objects/batch");
            HttpNode lfsApiNode = api.getRoot().node(lfsApiPath, null);

            Request request = new Request("POST", lfsApiNode);
            request.addRequestHeader("Accept", "application/vnd.git-lfs+json");
            request.addRequestHeader("Content-Type", "application/vnd.git-lfs+json");
            String payload = String.format("{\"operation\": \"download\", \"transfers\": [\"basic\"], \"objects\": [{\"oid\": \"%s\", \"size\": "
                    + "%d}]}", oid, size);
            Body body = new Body(null, null, payload.length(), new ByteArrayInputStream(payload.getBytes()), false);

            Response response = request.request(body);
            JsonObject all;
            JsonElement element = new JsonParser().parse(new String(response.getBodyBytes(), "UTF-8"));
            all = element.getAsJsonObject();
            String downloadUrl = null;
            for (JsonElement je: all.get("objects").getAsJsonArray()){
                downloadUrl = je.getAsJsonObject().get("actions").getAsJsonObject().get("download").getAsJsonObject().get("href").getAsString();
            }
            if (downloadUrl == null){
                throw new RuntimeException("Object for LFS link not found.");
            }
            // download file content and write into "dest"
            URL url = new URL(downloadUrl);
            BufferedInputStream fromStream = new BufferedInputStream(url.openStream());

            while ((bytesRead = fromStream.read(buffer)) != -1){
                dest.write(buffer, 0, bytesRead);
            }
        } else {
            // direct download from bitbucket
            while (bytesRead != -1){
                dest.write(buffer, 0, bytesRead);
                bytesRead = from.read(buffer);
            }
        }
    }

    private interface Collector {
        void add(JsonElement element);
    }

    public void getPaged(Collector collector, HttpNode node, String... params) throws IOException {
        HttpNode req;
        int start;
        JsonObject response;
        JsonArray values;

        start = 0;
        while (true) {
            req = node.getRoot().node(node.getPath(), query(start, 25, params));
            response = parser.parse(req.readString()).getAsJsonObject();
            values = response.get("values").getAsJsonArray();
            for (JsonElement element : values) {
                collector.add(element);
            }
            if (response.get("isLastPage").getAsBoolean()) {
                break;
            }
            start = response.get("nextPageStart").getAsInt();
        }
    }

    private static String query(int start, int limit, String... params) {
        StringBuilder result;

        result = new StringBuilder("start=" + start + "&limit=" + limit);
        for (int i = 0; i < params.length; i += 2) {
            result.append('&');
            result.append(params[i]);
            result.append('=');
            result.append(params[i + 1]);
        }
        return result.toString();
    }
}
