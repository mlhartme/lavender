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
package net.oneandone.lavender.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UsernamePasswordTest {
    @Test
    public void normal() {
        UsernamePassword up;

        up = new UsernamePassword("name", "pw");
        assertEquals(URI.create("svn:https://name:pw@server.org/foo/bar"), up.add(URI.create("svn:https://server.org/foo/bar")));
        assertEquals(URI.create("svn:https://name:pw@server.org:443/foo/bar"), up.add(URI.create("svn:https://server.org:443/foo/bar")));
        assertEquals(URI.create("svn:https://name:pw@server.org"), up.add(URI.create("svn:https://server.org")));
    }
}
