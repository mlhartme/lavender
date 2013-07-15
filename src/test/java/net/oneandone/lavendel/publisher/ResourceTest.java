package net.oneandone.lavendel.publisher;

import net.oneandone.lavendel.index.Hex;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceTest {

    private Resource resource;

    @Before
    public void setup() throws IOException {
        byte[] data = new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0x81, (byte) 0xFF };
        resource = new Resource(data, "modules/x/img/close.gif", "folder");
    }

    @Test
    public void testData() throws IOException {
        byte[] data = resource.getData();
        assertNotNull(data);
        assertEquals(6, data.length);
        assertEquals(0x00, data[0]);
        assertEquals(0x01, data[1]);
        assertEquals(0x7F, data[2]);
        assertEquals((byte) 0x80, data[3]);
        assertEquals((byte) 0x81, data[4]);
        assertEquals((byte) 0xFF, data[5]);
    }

    @Test
    public void testGetOriginalPath() {
        assertEquals("modules/x/img/close.gif", resource.getPath());
    }

    @Test
    public void testGetLavendelizedPath() {
        assertEquals("852/e7d76cdb8af7395cd039c0ecc293a/folder/close.gif", resource.labelLavendelized("").getLavendelizedPath());
    }

    @Test
    public void testMd5() {
        byte[] md5 = resource.md5();
        assertNotNull(md5);
        assertEquals(16, md5.length);
        assertEquals("852e7d76cdb8af7395cd039c0ecc293a", Hex.encodeString(md5));
    }

    @Test
    public void testToString() {
        String string = resource.toString();
        assertNotNull(string);
    }

}
