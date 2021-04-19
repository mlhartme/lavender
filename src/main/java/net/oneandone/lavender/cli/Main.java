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

import net.oneandone.inline.Cli;
import net.oneandone.inline.Console;
import net.oneandone.inline.commands.Help;
import net.oneandone.inline.commands.PackageVersion;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        System.exit(doMain(null, args));
    }

    public static class Commandline {
        public final String[] args;

        public Commandline(String... args) {
            this.args = args;
        }
    }

    public static int doMain(Globals globals, String... args) throws IOException {
        Console console;
        Cli cli;

        try (World world = World.create()) {
            console = Console.create();
            cli = new Cli(console::handleException);
            cli.primitive(FileNode.class, "file name", world.getWorking(), world::file);
            cli.begin(new Commandline(args));
            cli.begin(world);
            cli.begin(console, "-v -e { setVerbose(v) setStacktraces(e) }");
            cli.add(PackageVersion.class, "version");
            cli.addDefault(new Help(console, help()), "help");
            if (globals == null) {
                cli.begin(Globals.class, "-user=unknown@all -no-lock=false -await=600");
            } else {
                cli.begin(globals);
            }

            cli.add(War.class, "war war cluster docroot index");
            cli.add(Scm.class, "scm -prefix scm cluster docroot index?");
            cli.add(File.class, "file -prefix= archive cluster docroot index");
            cli.add(Direct.class, "direct cluster arg+");
            cli.add(Fsck.class, "fsck -md5 -gc -mac -repair-all-idx cluster");
            cli.add(RemoveEntry.class, "remove-entry cluster originalPath+");

            return cli.run(args);
        }
    }


    private static String help() {
        StringBuilder help;

        help = new StringBuilder();
        help.append("usage: 'lavender' global-options command\n");
        help.append("\n");
        help.append("publishing commands\n");
        help.append("  'war' war cluster docroot index\n");
        help.append("                            publish resources from the specified war, adds nodes- and index file to the war\n");
        help.append("                            index is a file name with suffix\n");
        help.append("  'scan-legacy' war         list all legacy modules in the specified war;\n");
        help.append("                            legacy is a pustefix module with embedded resources but not lavender.properties file\n");
        help.append("  'scm' ['-prefix' prefix] scm cluster docroot index?\n");
        help.append("                            publish resources from the specified scm\n");
        help.append("                            <scm> is searched in from host.properties;\n");
        help.append("                            index defaults to <scm> + '.idx'; prefix defaults to <scm> + '/'");
        help.append("  'file' ['-prefix' prefix] archive cluster docroot index\n");
        help.append("                            publish resources from archive to docroot on the specified cluster;\n");
        help.append("                            index is a file name with suffix; default prefix is empty;\n");
        help.append("                            archive is simply a directory or an zip archive (e.g. a jar- or zip file)\n");
        help.append("other commands\n");
        help.append("  'help'                    print this message\n");
        help.append("  'version'                 print version information\n");
        help.append("  'direct' cluster arg+     executes the specified command on all machines of the cluster\n");
        help.append("  'fsck' ['-md5'] ['-gc'] ['-repair-all-idx'] cluster\n");
        help.append("                            checks if all files are indexed and referenced and the same on all machines\n");
        help.append("                            -md5 also checks md5 sums of every file;\n");
        help.append("                            -gc removes unreferenced files\n");
        help.append("                            -repair-all-idx automatically fixes the all index if it is broken\n");
        help.append("  'remove-entry' cluster originalPath+\n");
        help.append("                            removes the specified entries from from all indexes where it is found\n");
        help.append("                            Note that the referenced file is not deleted - that's up to the next gc run.\n");
        help.append("global options\n");
        help.append("  '-await' seconds          how long to wait for a lock before giving up; default is 600\n");
        help.append("  '-user' email             written to lock files to know who's currently holding the lock; defaults to unknown@all\n");

        return help.toString();
    }

    private Main() {
    }
}
