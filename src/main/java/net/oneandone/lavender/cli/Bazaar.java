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
package net.oneandone.lavender.cli;

import com.jcraft.jsch.JSchException;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Connection;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Pool;
import net.oneandone.lavender.config.Settings;
import net.oneandone.lavender.config.Target;
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

    public Bazaar(Console console, Settings settings, Net base) {
        super(console, settings, base);
    }

    @Override
    public void invoke() throws Exception {
        Cluster cluster;
        Docroot docroot;
        Target target;
        SshNode srcdir;
        SshNode srcfile;
        FileNode local;
        ZipNode root;
        Node destTmp;
        Node destFinal;

        cluster = net.get("internal");
        docroot = cluster.docroot(Docroot.SVN);
        target = new Target(cluster, docroot, docroot.aliases().get(0));
        srcdir = feeds(console.world);
        srcdir.join("bv_1und1_smartseo.zip.ready").checkFile();
        local = console.world.getTemp().createTempFile();
        srcfile = srcdir.join("bv_1und1_smartseo.zip");
        srcfile.copyFile(local);
        console.info.println("downloaded " + local.length() + " bytes from " + srcfile.getURI());
        root = local.openZip();
        try (Pool pool = pool()) {
            for (Connection connection : target.cluster.connect(pool)) {
                console.info.println(connection.getHost());
                destTmp = target.docroot.node(connection).getParent().join("bazaarvoice/tmp").mkdirsOpt();
                destFinal = destTmp.getParent().join("latest");
                destFinal.checkDirectory();
                destTmp.mkdir();
                root.copyDirectory(destTmp);
                destFinal.deleteTree();
                destTmp.move(destFinal);
            }
        }
    }
}
