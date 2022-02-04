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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpFilesystem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GiteaScmRootIT {
    private static final World WORLD;

    static {
        try {
            WORLD = World.create(false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void readTextFile() throws Exception {
        GiteaScmRoot r;

        r = GiteaScmRoot.create(WORLD, URI.create("https://git.ionos.org/CP-DevEnv/application-parent-pom.git"), "main", null);
        System.out.println("test: " + new String(r.read("pom.xml"), StandardCharsets.UTF_8));
    }

    @Test
    public void readImage() throws Exception {
        GiteaScmRoot r;

        // HttpFilesystem.wireLog("wire.log");
        r = GiteaScmRoot.create(WORLD, URI.create("https://git.ionos.org/mhm/lavender-test-module.git"), "main", null);
        assertTrue(r.read("Penny_test.jpg").length > 0);
    }
}
