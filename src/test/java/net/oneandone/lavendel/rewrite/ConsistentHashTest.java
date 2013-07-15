package net.oneandone.lavendel.rewrite;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsistentHashTest {

    private ConsistentHash ch;

    @Before
    public void setUp() {
        ch = new ConsistentHash(200, new String[] { "n1", "n2", "n3", "n4" });
    }

    @Test
    public void testAddNode() {
        assertEquals(800, ch.circle.size());

        ch.addNode("n5");
        assertEquals(1000, ch.circle.size());
    }

    @Test
    public void testRemoveNode() {
        assertEquals(800, ch.circle.size());

        ch.removeNode("n1");
        assertEquals(600, ch.circle.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testEmptyCircle() {
        ConsistentHash consistentHash = new ConsistentHash(200, new String[] {});

        String s = "";
        byte[] md5 = DigestUtils.md5(s);
        consistentHash.getNodeForHash(md5);
    }

    @Test
    public void testKeyString() {
        String s = "";
        String md5Hex = DigestUtils.md5Hex(s);
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5Hex);

        Integer key = ch.key(s);

        String keyHex = Integer.toHexString(key);
        assertEquals("d41d8cd9", keyHex);
        assertTrue(md5Hex.startsWith(keyHex));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyByteArrayNoMd5() {
        // sha is 16 bytes
        byte[] sha = DigestUtils.sha("");
        ch.key(sha);
    }

    @Test
    public void testKeyByteArray() {
        String s = "";
        byte[] md5 = DigestUtils.md5(s);
        String md5Hex = DigestUtils.md5Hex(s);
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5Hex);

        Integer key = ch.key(md5);

        String keyHex = Integer.toHexString(key);
        assertEquals("d41d8cd9", keyHex);
        assertTrue(md5Hex.startsWith(keyHex));
    }

    @Test
    public void testEqualDistribution() {

        Map<String, AtomicInteger> map = new TreeMap<String, AtomicInteger>();

        // calculate the nodes for 1000000 MD5 hashes
        // count how often a node is used
        for (int i = 0; i < 1000000; i++) {
            String s = "" + i;
            byte[] md5 = DigestUtils.md5(s);
            String node = ch.getNodeForHash(md5);

            if (!map.containsKey(node)) {
                map.put(node, new AtomicInteger());
            }
            map.get(node).incrementAndGet();
        }

        // now check equal distribution of nodes
        int count = 0;
        for (String node : map.keySet()) {
            AtomicInteger atomicInteger = map.get(node);
            assertTrue(atomicInteger.get() > 230000);
            assertTrue(atomicInteger.get() < 270000);
            count += atomicInteger.get();
        }
        assertEquals(1000000, count);
    }

}
