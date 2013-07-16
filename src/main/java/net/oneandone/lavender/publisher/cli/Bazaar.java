/**
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
package net.oneandone.lavender.publisher.cli;

import com.jcraft.jsch.JSchException;
import net.oneandone.lavender.publisher.config.Host;
import net.oneandone.lavender.publisher.config.Net;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.fs.ssh.SshNode;
import net.oneandone.sushi.fs.ssh.SshRoot;
import net.oneandone.sushi.fs.zip.ZipNode;

import java.io.IOException;

/** See http://issue.tool.1and1.com/browse/ITOSHA-5455 */
public class Bazaar extends Base {
    public static SshNode feeds(World world) throws JSchException {
        SshFilesystem fs;

        fs = (SshFilesystem) world.getFilesystem("ssh");
        return new SshRoot(fs, "sftp.1und1.de", 1022, "bazaarvoice", fs.getDefaultTimeout()).node("feeds", null);
    }

    public Bazaar(Console console, Net base) {
        super(console, base);
    }

    @Override
    public void invoke() throws Exception {
        SshNode dir;
        SshNode src;
        FileNode local;
        ZipNode root;
        Node destTmp;
        Node destFinal;

        dir = feeds(console.world);
        if (!dir.join("bv_1und1_smartseo.zip.ready").isFile()) {
            throw new IOException("not ready");
        }
        local = console.world.getTemp().createTempFile();
        src = dir.join("bv_1und1_smartseo.zip");
        src.copyFile(local);
        console.info.println("downloaded " + local.length() + " bytes from " + src.getURI());
        root = local.openZip();
        for (Host host : net.cluster("bazaar").hosts) {
            console.info.println(host);
            destTmp = host.docroot(host.open(console.world), "/var/bazaarvoice/tmp");
            destFinal = destTmp.getParent().join("latest");
            destFinal.checkDirectory();
            destTmp.mkdir();
            root.copyDirectory(destTmp);
            destFinal.deleteTree();
            destTmp.move(destFinal);
        }
    }
}
