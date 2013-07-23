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
package net.oneandone.lavender.filter.processor;

import net.oneandone.lavender.index.Index;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @link http://weblogs.java.net/blog/2007/11/27/consistent-hashing"
 */
public class ConsistentHash {

    protected final int numberOfReplicas;
    protected final SortedMap<Integer, String> circle = new TreeMap<>();

    public ConsistentHash(int numberOfReplicas) {
        this(numberOfReplicas, new String[0]);
    }

    public ConsistentHash(int numberOfReplicas, String[] nodes) {
        this.numberOfReplicas = numberOfReplicas;

        for (String node : nodes) {
            addNode(node);
        }
    }

    public void addNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(key(node + "-" + i), node);
        }
    }

    public void removeNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(key(node + "-" + i));
        }
    }

    /**
     * Gets the node for a MD5 hash.
     * @param md5
     *            the MD5 hash
     * @return the node
     */
    public String getNodeForHash(byte[] md5) {
        if (circle.isEmpty()) {
            throw new IllegalStateException("No node in hash circle.");
        }
        int key = key(md5);
        SortedMap<Integer, String> tailMap = circle.tailMap(key);
        key = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        return circle.get(key);
    }

    /**
     * Calculates the circle key of the given string.
     * @param s
     *            the string
     * @return the circle key
     */
    protected Integer key(String s) {
        return key(md5(s));
    }

    /**
     * Calculates the circle key of the given MD5 hash. The given MD5 hash must have a length of 16 bytes.
     * @param md5
     *            the MD5 hash
     * @return the circle key
     */
    protected Integer key(byte[] md5) {
        if (md5.length != 16) {
            throw new IllegalArgumentException("Expected a 16 byte / 128 bit hash.");
        }

        int key = (md5[0] & 0xFF) << 24 | (md5[1] & 0xFF) << 16 | (md5[2] & 0xFF) << 8 | (md5[3] & 0xFF);

        return key;
    }

    public static byte[] md5(String data) {
        byte[] bytes;
        MessageDigest digest;

        try {
            bytes = data.getBytes(Index.ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return digest.digest(bytes);
    }
}
