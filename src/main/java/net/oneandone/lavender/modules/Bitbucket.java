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
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;

/** As of 8-2018, we have bitbucket server 5.9.1.
 *  Rest API documentation: https://docs.atlassian.com/bitbucket-server/rest/5.12.0/bitbucket-rest.html?utm_source=%2Fstatic%2Frest%2Fbitbucket-server%2Flatest%2Fbitbucket-rest.html&utm_medium=301  */
public class Bitbucket {
    public static void main(String[] args) throws IOException {
        World world;
        Bitbucket bitbucket;
        JsonObject obj;
        JsonArray values;
        JsonObject path;
        String full;

        world = World.create(false);
        bitbucket = new Bitbucket((HttpNode) world.validNode("http://bitbucket.1and1.org:7990/rest/api/1.0"));
        System.out.println("branches" + bitbucket.branches("CISOOPS", "puc"));
        obj = bitbucket.changes("CISOOPS", "puc", "ea01f95dafd", "cd309b8a877");
        values = obj.get("values").getAsJsonArray();
        for (JsonElement element : values) {
            path = element.getAsJsonObject().get("path").getAsJsonObject();
            full = path.get("parent").getAsString();
            if (!full.isEmpty()) {
                full = full + "/";
            };
            full = full + path.get("name").getAsString();
            System.out.println(full);

        }
    }

    private final HttpNode root;
    private final JsonParser parser;

    public Bitbucket(HttpNode root) {
        this.root = root;
        this.parser = new JsonParser();
    }

    public String branches(String project, String repository) throws IOException {
        return root.join("projects", project, "repos", repository, "branches").readString();
    }

    public JsonObject changes(String project, String repository, String from, String to) throws IOException {
        return parser.parse(root.getRoot().node(root.getPath() + "/projects/" + project + "/repos/" + repository + "/compare/changes", "from=" + from + "&to=" + to).readString()).getAsJsonObject();
    }
}
