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
package net.oneandone.lavender.publisher;

import com.jcraft.jsch.JSchException;
import net.oneandone.lavender.config.Cluster;
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Host;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Settings;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshNode;
import net.oneandone.sushi.fs.ssh.SshRoot;
import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Verify extends Base {
    @Value(name = "cluster", position = 1)
    private String clusterName;

    public Verify(Console console, Settings settings, Net net) {
        super(console, settings, net);
    }

    @Override
    public void invoke() throws IOException {
        Cluster cluster;
        Node hostroot;
        Index index;
        List<String> files;
        Set<String> references;
        Node docroot;
        List<String> tmp;
        boolean problem;

        problem = false;
        cluster = net.cluster(clusterName);
        for (Host host : cluster.hosts) {
            hostroot = host.open(console.world);
            for (Docroot docrootObj : cluster.docroots) {
                docroot = docrootObj.node(hostroot);
                if (docroot.exists()) {
                    references = new HashSet<>();
                    console.info.println(host);
                    console.info.print("collecting files ...");
                    files = find(docroot, "-type", "f");
                    console.info.println("done: " + files.size());
                    console.info.print("collecting references ...");
                    for (Node file : docrootObj.indexDirectory(hostroot).list()) {
                        index = Index.load(file);
                        for (Label label : index) {
                            references.add(label.getLavendelizedPath());
                        }
                    }
                    console.info.println("done: " + references.size());

                    tmp = new ArrayList<>(references);
                    tmp.removeAll(files);
                    if (!tmp.isEmpty()) {
                        problem = true;
                        console.error.println("not existing references: " + tmp);
                    }
                    tmp = new ArrayList<>(files);
                    tmp.removeAll(references);
                    if (!tmp.isEmpty()) {
                        problem = true;
                        console.error.println("not referenced files: " + tmp);
                    }
                }
            }
        }
        if (problem) {
            throw new IOException("verify failed");
        } else {
            console.info.println("verify ok");
        }
    }

    public static List<String> find(Node base, String ... args) throws IOException {
        String str;
        List<String> lst;

        try {
            if (base instanceof SshNode) {
                str = ((SshRoot) base.getRoot()).exec(Strings.append(
                        new String[] { "cd", "/" + base.getPath(), "&&", "find", "." }, args));
            } else if (base instanceof FileNode) {
                str = ((FileNode) base).exec(Strings.append(new String[] { "find", "." }, args));
            } else {
                throw new UnsupportedOperationException("find on " + base.getClass());
            }
        } catch (JSchException e) {
            throw new IOException("error obtaining file list: " + e.getMessage(), e);
        }
        lst = new ArrayList<>();
        for (String path : OS.CURRENT.lineSeparator.split(str)) {
            if (path.startsWith("./")) {
                path = path.substring(2);
            }
            path = path.trim();
            if (!path.isEmpty()) {
                lst.add(path);
            }
        }
        return lst;
    }
}
