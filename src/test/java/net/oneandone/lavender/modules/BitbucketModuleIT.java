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

import net.oneandone.lavender.config.Secrets;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitbucketModuleIT {
    private static final World WORLD;

    static {
        try {
            WORLD = World.create(false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void directory() throws Exception {
        Secrets secrets;
        Module<?> module;
        Iterator<Resource> iter;
        Resource resource;
        FileNode file;
        ByteArrayOutputStream normal;
        ByteArrayOutputStream lfs;

        file = WORLD.file("it.secrets");
        if (!file.exists() && file.getParent().getName().equals("checkout")) {
            // release build - fix patch
            file = file.getParent().getParent().getParent().join(file.getName());
        }
        if (!file.isFile()) {
            throw new IOException("not found: " + file.getAbsolute());
        }
        secrets = new Secrets();
        secrets.addAll(file);
        module = new BitbucketModule(Bitbucket.create(WORLD, "bitbucket.1and1.org", secrets.lookup("git")),
                "CISOOPS", "lavender-test-module", "master", "", "myname", null,false,
                "", "", WORLD.filter().include("**/*.jpg", "**/*.css"), null);

        assertNull(module.probe("no/such.file"));
        assertNotNull(module.probe("Penny_test.jpg"));

        iter = module.iterator();
        resource = iter.next();
        assertEquals("empty.css", resource.getPath());
        resource = iter.next();
        assertEquals("Penny_test.jpg", resource.getPath());
        normal = new ByteArrayOutputStream();
        resource.writeTo(normal);
        resource = iter.next();
        assertEquals("lfs/lfs-test.jpg", resource.getPath());
        lfs = new ByteArrayOutputStream();
        resource.writeTo(lfs);
        assertTrue(Arrays.equals(normal.toByteArray(), lfs.toByteArray()));
        assertFalse(iter.hasNext());
    }
}
