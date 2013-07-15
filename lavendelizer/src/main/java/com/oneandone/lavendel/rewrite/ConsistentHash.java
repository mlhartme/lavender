package com.oneandone.lavendel.rewrite;

import com.oneandone.lavendel.index.Index;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @see http://weblogs.java.net/blog/2007/11/27/consistent-hashing"
 * @author seelmann
 */
public class ConsistentHash {

    protected final int numberOfReplicas;
    protected final SortedMap<Integer, String> circle = new TreeMap<Integer, String>();

    /**
     * Instantiates a new consistent hash.
     * @param numberOfReplicas
     *            the number of replicas
     * @param nodes
     *            the nodes
     */
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
