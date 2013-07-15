package com.oneandone.lavendel.publisher.cli;

import com.jcraft.jsch.JSchException;
import com.oneandone.lavendel.publisher.config.Host;
import com.oneandone.lavendel.publisher.config.Net;
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

    public Direct(Console console, Net net) {
        super(console, net);
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
            root = fs.root(host.name, host.login);
            console.info.println(host.toString());
            try {
                console.info.println(root.exec(Strings.toArray(command)));
            } catch (ExitCode e) {
                console.error.println(e.toString());
            }
        }
    }
}
