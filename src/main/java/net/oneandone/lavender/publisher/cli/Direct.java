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
import net.oneandone.lavender.config.Host;
import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Settings;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.fs.ssh.SshRoot;
import net.oneandone.sushi.launcher.ExitCode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Direct extends Base {
    private final List<String> command;

    @Value(name = "cluster", position = 1)
    private String cluster;

    @Remaining(name = "hosts")
    public void command(String arg) {
        command.add(arg);
    }

    public Direct(Console console, Settings settings, Net net) {
        super(console, settings, net);
        this.command = new ArrayList<>();
    }

    @Override
    public void invoke() throws IOException, JSchException {
        SshFilesystem fs;
        SshRoot root;

        if (command.size() == 0) {
            throw new ArgumentException("missing command");
        }
        fs = (SshFilesystem) console.world.getFilesystem("ssh");
        for (Host host : net.cluster(cluster).hosts) {
            root = (SshRoot) host.open(console.world).getRoot();
            console.info.println(host.toString());
            try {
                console.info.println(root.exec(Strings.toArray(command)));
            } catch (ExitCode e) {
                console.error.println(e.toString());
            }
        }
    }
}
