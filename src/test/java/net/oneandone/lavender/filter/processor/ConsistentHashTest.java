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
package net.oneandone.lavender.filter.processor;

import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.index.Util;
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
        byte[] md5 = Util.md5();
        consistentHash.getNodeForHash(md5);
    }

    @Test
    public void testKeyString() {
        String md5Hex = Hex.encodeString(Util.md5());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5Hex);

        Integer key = ch.key("");

        String keyHex = Integer.toHexString(key);
        assertEquals("d41d8cd9", keyHex);
        assertTrue(md5Hex.startsWith(keyHex));
    }

    @Test
    public void testKeyByteArray() {
        byte[] md5 = Util.md5();
        String md5Hex = Hex.encodeString(md5);
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
            byte[] md5 = Util.md5(s.getBytes());
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
