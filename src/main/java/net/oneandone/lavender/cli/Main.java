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

import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Settings;
import net.oneandone.sushi.cli.Child;
import net.oneandone.sushi.cli.Cli;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Main extends Cli implements Command {
    public static void main(String[] args) throws IOException {
        Settings settings;

        settings = Settings.load();
        System.exit(doMain(settings, null, args));
    }

    public static int doMain(Settings settings, Net net, String ... args) {
        Main main;

        main = new Main(settings, net);
        return main.run(args);
    }

    @Option("lastconfig")
    private boolean lastConfig;

    private Net lazyNet;
    private final Settings settings;

    public Main(Settings settings, Net net) {
        super(settings.world);
        this.settings = settings;
        this.lazyNet = net;
    }

    @Child("war")
    public Command war() throws IOException {
        return new War(console, settings, net());
    }

    @Child("svn")
    public Command svn() throws IOException {
        return new Svn(console, settings, Strings.removeLeft(settings.svn.toString(), "svn:"), net());
    }

    @Child("file")
    public Command file() throws IOException {
        return new File(console, settings, net());
    }

    @Child("direct")
    public Command direct() throws IOException {
        return new Direct(console, settings, net());
    }

    @Child("gc")
    public Command gc() throws IOException {
        return new GarbageCollection(console, settings, net());
    }

    @Child("validate")
    public Command validate() throws IOException {
        return new Validate(console, settings, net());
    }

    @Override
    public void printHelp() {
        console.info.println("usage: 'lavender' command");
        console.info.println();
        console.info.println("publishing commands");
        console.info.println("  'war' inputWar outputWar idxName target+");
        console.info.println("                            publish resources from the specified war,");
        console.info.println("                            target = type '=' cluster ['/' alias]");
        console.info.println("  'svn' ['-type' type] directory cluster");
        console.info.println("                            publish resources from svn to the specified cluster; type defaults to 'svn'");
        console.info.println("  'file' ['-prefix' prefix] file idxName type cluster");
        console.info.println("                            publish resources from file to the specified cluster");
        console.info.println("other commands");
        console.info.println("  'help'                    print this message");
        console.info.println("  'version'                 print version information");
        console.info.println("  'direct' cluster arg+     executes the specified command on all machines of the cluster");
        console.info.println("  'validate' cluster        checks if all files are indexed and indexes match");
        console.info.println("  'gc' ['-dryrun'] cluster  removes unreferenced resources and empty directories from the");
        console.info.println("                            specified host(s); default: all hosts");
    }

    @Override
    public void invoke() {
        printHelp();
    }

    private Net net() throws IOException {
        if (lazyNet == null) {
            lazyNet = lastConfig ? settings.loadLastNet() : settings.loadNet();
        }
        return lazyNet;
    }
}
