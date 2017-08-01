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
package net.oneandone.lavender.cli;

import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Properties;
import net.oneandone.sushi.cli.Child;
import net.oneandone.sushi.cli.Cli;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Main extends Cli implements Command {
    public static void main(String[] args) throws IOException {
        Properties properties;

        properties = Properties.load();
        System.exit(doMain(properties, null, args));
    }

    public static int doMain(Properties properties, Net net, String ... args) {
        Main main;

        main = new Main(properties, net);
        return main.run(args);
    }

    @Option("lastconfig")
    private boolean lastConfig;

    private Net lazyNet;
    private final Properties properties;

    public Main(Properties properties, Net net) {
        super(properties.world);
        this.properties = properties;
        this.lazyNet = net;
    }

    @Child("war")
    public Command war() throws IOException {
        return new War(console, properties, net());
    }

    @Child("svn")
    public Command svn() throws IOException {
        return new Svn(console, properties, Strings.removeLeft(properties.svn.toString(), "svn:"), net());
    }

    @Child("file")
    public Command file() throws IOException {
        return new File(console, properties, net());
    }

    @Child("direct")
    public Command direct() throws IOException {
        return new Direct(console, properties, net());
    }

    @Child("fsck")
    public Command fsck() throws IOException {
        return new Fsck(console, properties, net());
    }

    @Child("remove-entry")
    public Command removeEntry() throws IOException {
        return new RemoveEntry(console, properties, net());
    }

    @Override
    public void printHelp() {
        console.info.println("usage: 'lavender' command");
        console.info.println();
        console.info.println("publishing commands");
        console.info.println("  'war' global-options war idxName target+");
        console.info.println("                            publish resources from the specified war, addes nodes- and index file to the war;");
        console.info.println("                            target = type '=' cluster ['/' alias]");
        console.info.println("  'svn' global-options ['-type' type] directory cluster");
        console.info.println("                            publish resources from <svn>/data/<directory> to <docroot>/<directory>");
        console.info.println("                            type defaults to 'svn'; <svn> is picked from lavender.properties");
        console.info.println("  'file' global-options ['-prefix' prefix] archive idxName type cluster");
        console.info.println("                            publish resources from archive to the specified cluster;");
        console.info.println("                            archive is simply a directory or an zip archive (e.g. a jar- or zip file)");
        console.info.println("other commands");
        console.info.println("  'help'                    print this message");
        console.info.println("  'version'                 print version information");
        console.info.println("  'direct' cluster arg+     executes the specified command on all machines of the cluster");
        console.info.println("  'fsck' ['-md5'] ['-gc'] cluster");
        console.info.println("                            checks if all files are indexed and referenced and the same on all machines");
        console.info.println("  'remove-entry' cluster originalPath+");
        console.info.println("                            removes the specified entries from from all indexes where it is found");
        console.info.println("                            Note that the referenced file is not deleted - that's up to the next gc run.");
        console.info.println("global options");
        console.info.println("  '-await' seconds          how long to wait for a lock before giving up; default is 600");
        console.info.println("  '-user' email             written to lock files to know who's currently holding the lock; defaults to unknown@all");
    }

    @Override
    public void invoke() {
        printHelp();
    }

    private Net net() throws IOException {
        if (lazyNet == null) {
            lazyNet = lastConfig ? properties.loadLastNet() : properties.loadNet();
        }
        return lazyNet;
    }
}
