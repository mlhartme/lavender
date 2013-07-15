package net.oneandone.lavendel.publisher.svn;

import org.junit.Test;

import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SvnResourceExtractorConfigTest {
    @Test
    public void empty() {
        assertEquals(0, SvnExtractorConfig.parse(new Properties()).size());
    }

    @Test
    public void one() {
        Properties props;
        Collection<SvnExtractorConfig> result;
        SvnExtractorConfig config;

        props = new Properties();
        props.put("svn.foo", "svn");
        props.put("svn.foo.pathPrefix", "prefix");
        props.put("svn.foo.lavendelize", "false");
        result = SvnExtractorConfig.parse(props);
        assertEquals(1, result.size());
        config = result.iterator().next();
        assertEquals("foo", config.name);
        assertFalse(config.lavendelize);
        assertEquals("prefix", config.pathPrefix);
        assertEquals("svn", config.svn);
    }

    @Test
    public void more() {
        Properties props;
        Collection<SvnExtractorConfig> result;

        props = new Properties();
        props.put("svn.foo", "1");
        props.put("svn.bar", "2");
        props.put("svn.baz", "3");
        result = SvnExtractorConfig.parse(props);
        assertEquals(3, result.size());
    }

    @Test
    public void simplify() {
        check("", "");
        check("ab/cd/ef", "ab/cd/ef");
        check("a/b", "a/trunk/b");
        check("a/b", "a/tags/foo-1.2/b");
    }

    private void check(String expected, String orig) {
        assertEquals(expected, SvnExtractorConfig.simplify(orig));
    }
}
