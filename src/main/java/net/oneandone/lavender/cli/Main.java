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

import java.io.IOException;

public class Main extends Cli implements Command {
    public static void main(String[] args) throws IOException {
        Settings settings;

        settings = Settings.load();
        System.exit(doMain(settings, settings.loadNet(), args));
    }

    public static int doMain(Settings settings, Net net, String ... args) throws IOException {
        Main main;

        main = new Main(settings, net);
        return main.run(args);
    }

    private final Net net;
    private final Settings settings;

    public Main(Settings settings, Net net) throws IOException {
        super(settings.world);
        this.settings = settings;
        this.net = net;
    }

    @Child("war")
    public Command war() {
        return new War(console, settings, net);
    }

    @Child("svn")
    public Command svn() {
        return new Svn(console, settings, net);
    }

    @Child("direct")
    public Command direct() {
        return new Direct(console, settings, net);
    }

    @Child("bazaar")
    public Command bazaar() {
        return new Bazaar(console, settings, net);
    }

    @Child("gc")
    public Command gc() {
        return new GarbageCollection(console, settings, net);
    }

    @Child("verify")
    public Command verfiy() {
        return new Verify(console, settings, net);
    }

    @Override
    public void printHelp() {
        console.info.println("usage: 'lavender' command");
        console.info.println();
        console.info.println("publishing commands");
        console.info.println("  'war' inputWar outputWar idxName target+");
        console.info.println("                            publish resources from the specified war,");
        console.info.println("                            target = type '=' cluster ['/' alias]");
        console.info.println("  'svn' directory cluster   publish resources from svn to the specified cluster");
        console.info.println("other commands");
        console.info.println("  'help'                    print this message");
        console.info.println("  'version'                 print version information");
        console.info.println("  'bazaar'                  publishes Bazaar Voice files to eu cluster.");
        console.info.println("                            actually publish to the specified cluster");
        console.info.println("  'direct' cluster arg+     executes the specified command on all machines of the cluster");
        console.info.println("  'verify' cluster          checks if all files are indexed and indexes match");
        console.info.println("  'gc' ['-dryrun'] cluster  removes unreferenced resources and empty directories from the");
        console.info.println("                            specified host(s); default: all hosts");
    }

    @Override
    public void invoke() {
        printHelp();
    }
}
