package net.oneandone.lavendel.publisher.cli;


import net.oneandone.lavendel.publisher.config.Net;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Console;

public abstract class Base implements Command {
    protected final Console console;
    protected final Net net;

    protected Base(Console console, Net net) {
        this.console = console;
        this.net = net;
    }

    @Override
    public abstract void invoke() throws Exception;
}
