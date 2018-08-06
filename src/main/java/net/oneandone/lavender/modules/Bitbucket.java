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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;

/** As of 8-2018, we have bitbucket server 5.9.1.
 *  Rest API documentation: https://developer.atlassian.com/server/bitbucket/reference/rest-api/ */
public class Bitbucket {
    public static void main(String[] args) throws IOException {
        World world;
        Bitbucket bitbucket;

        world = World.create(false);
        bitbucket = new Bitbucket((HttpNode) world.validNode("http://bitbucket.1and1.org:7990/rest/api/1.0"));
        System.out.println(bitbucket.changes("CISOOPS", "puc", "ea01f95dafd", "cd309b8a877"));
    }

    private final HttpNode root;
    private final JsonParser parser;

    public Bitbucket(HttpNode root) {
        this.root = root;
        this.parser = new JsonParser();
    }

    public JsonObject changes(String project, String repository, String from, String to) throws IOException {
        return parser.parse(root.getRoot().node(root.getPath() + "/projects/" + project + "/repos/" + repository + "/compare/changes", "from=" + from + "&to=" + to).readString()).getAsJsonObject();
    }
}
