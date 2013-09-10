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
import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Host;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Settings;
import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.index.Label;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshNode;
import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Verify extends Base {
    @Option("md5")
    private boolean md5check;

    @Option("mac")
    private boolean mac;

    @Value(name = "cluster", position = 1)
    private String clusterName;

    public Verify(Console console, Settings settings, Net net) {
        super(console, settings, net);
    }

    @Override
    public void invoke() throws IOException {
        Cluster cluster;
        Node hostroot;
        Node docroot;
        boolean problem;

        problem = false;
        cluster = net.get(clusterName);
        for (Host host : cluster.hosts()) {
            console.info.println(host);
            hostroot = host.open(console.world);
            for (Docroot docrootObj : cluster.docroots()) {
                docroot = docrootObj.node(hostroot);
                if (docroot.exists()) {
                    if (filesAndReferences(hostroot, docroot, docrootObj)) {
                        problem = true;
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

    private boolean filesAndReferences(Node hostroot, Node docroot, Docroot docrootObj) throws IOException {
        boolean problem;
        Set<String> references;
        List<String> files;
        Index index;
        List<String> tmp;
        Index all;
        Index allLoaded;
        Node fixed;

        problem = false;
        references = new HashSet<>();
        console.info.println(docroot.getURI().toString());
        console.info.print("  collecting files ...");
        files = find(docroot, "-type", "f");
        console.info.println("done: " + files.size());
        console.info.print("  collecting references ...");
        all = new Index();
        for (Node file : docrootObj.indexList(hostroot)) {
            index = Index.load(file);
            for (Label label : index) {
                references.add(label.getLavendelizedPath());
                all.addReference(label.getLavendelizedPath(), label.md5());
            }
        }
        allLoaded = Index.load(docrootObj.index(hostroot, Index.ALL_IDX));
        if (!all.equals(allLoaded)) {
            fixed = docrootObj.index(hostroot, Index.ALL_IDX + ".fixed");
            console.error.println("all-index is broken, saving fixed to " + fixed);
            all.save(fixed);
            problem = true;
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
        if (md5check) {
            if (md5check(docroot, all)) {
                problem = true;
            }
        }
        return problem;
    }

    private boolean md5check(Node docroot, Index index) throws IOException {
        boolean problem;
        List<String> paths;
        List<String> expecteds;

        problem = false;
        console.info.println("  md5 check ...");
        paths = new ArrayList<>();
        expecteds = new ArrayList<>();
        for (Label label : index) {
            paths.add(label.getOriginalPath());
            expecteds.add(Hex.encodeString(label.md5()));
            if (paths.size() > 500) {
                if (md5check(docroot, paths, expecteds)) {
                    problem = true;
                }
                paths.clear();
                expecteds.clear();
            }
        }
        if (paths.size() > 0) {
            if (md5check(docroot, paths, expecteds)) {
                problem = true;
            }
        }
        console.info.println("  done");
        return problem;
    }

    private static final Separator SEPARATOR = Separator.RAW_LINE;

    private boolean md5check(Node docroot, List<String> paths, List<String> expecteds) throws IOException {
        String md5all;
        List<String> computeds;
        boolean problem;
        int i;
        String expected;

        problem = false;
        md5all = exec(docroot, Strings.append(mac ? new String[] { "md5", "-q" } : new String[] { "md5sum" }, Strings.toArray(paths)));
        computeds = SEPARATOR.split(md5all);
        if (expecteds.size() != computeds.size()) {
            throw new IllegalStateException(expecteds + " vs " + computeds);
        }
        i = 0;
        for (String computed : computeds) {
            computed = computed.trim();
            if (!mac) {
                // because md5sum prints the checksum followed by the path
                computed = computed.substring(0, computed.indexOf(' '));
            }
            expected = expecteds.get(i);
            if (!expected.equals(computed)) {
                console.error.println(paths.get(i)+ ": md5 broken: expected " + expected + ", got " + computed);
                problem = true;
            }
            i++;
        }
        return problem;
    }

    public static List<String> find(Node base, String ... args) throws IOException {
        String str;
        List<String> lst;

        str = exec(base, Strings.append(new String[] { "find", "." }, args));
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

    private static String exec(Node dir, String ... cmd) throws IOException {
        if (dir instanceof SshNode) {
            try {
                return ((SshNode) dir).getRoot().exec(Strings.append(new String[] { "cd", "/" + dir.getPath(), "&&" }, escape(cmd)));
            } catch (JSchException e) {
                throw new IOException();
            }
        } else if (dir instanceof FileNode) {
            return ((FileNode) dir).exec(cmd);
        } else {
            throw new UnsupportedOperationException("exec on " + dir.getClass());
        }
    }

    // TODO: jsch problem -- it takes the argument as a string ...
    private static String[] escape(String[] args) {
        String[] result;
        String arg;

        result = new String[args.length];
        for (int i = 0; i < result.length; i++) {
            arg = args[i];
            if (arg.contains(" ")) {
                arg = "\"" + arg + "\"";
            }
            result[i] = arg;
        }
        return result;
    }
}
