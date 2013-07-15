package net.oneandone.lavendel.index;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LabelTest {

    private Label label;

    @Before
    public void setup() {
        byte[] data = new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0x81, (byte) 0xFF };
        byte[] md5 = DigestUtils.md5(data);
        String md5hex = DigestUtils.md5Hex(data);
        label = new Label("modules/x/img/close.gif", "x/" + md5hex + "-close.gif", md5);
    }

    @Test
    public void testGetOriginalPath() {
        assertEquals("modules/x/img/close.gif", label.getOriginalPath());
    }

    @Test
    public void testGetLavendelizedPath() {
        String lavendelizedPath = label.getLavendelizedPath();
        assertNotNull(lavendelizedPath);
        assertEquals("x/852e7d76cdb8af7395cd039c0ecc293a-close.gif", lavendelizedPath);
    }

    @Test
    public void testMd5() {
        byte[] md5 = label.md5();
        assertNotNull(md5);
        assertEquals(16, md5.length);
        assertEquals("852e7d76cdb8af7395cd039c0ecc293a", Hex.encodeString(md5));
    }

    @Test
    public void testToString() {
        String string = label.toString();
        assertNotNull(string);
    }

}
