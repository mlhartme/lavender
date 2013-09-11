package net.oneandone.lavender.config;

import net.oneandone.sushi.fs.World;
import org.junit.Test;

import java.io.IOException;

public class NetTest {
    @Test
    public void normal() throws IOException {
        Net.normal(new World());
    }
}
