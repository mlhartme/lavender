package net.oneandone.lavender.config;

import org.junit.Test;

import java.io.IOException;

public class NetTest {
    @Test
    public void normal() throws IOException {
        Settings.load().loadNet();
    }
}
