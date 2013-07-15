package com.oneandone.lavendel.publisher;

import com.oneandone.lavendel.index.Hex;
import com.oneandone.lavendel.index.Label;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Resource {
    private final byte[] data;
    private final String path;
    private final String folderName;

    public Resource(byte[] data, String path, String folderName) {
        this.data = data;
        this.path = path;
        this.folderName = folderName;
    }

    public byte[] getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path + "[" + data.length + "]";
    }


    public Label labelLavendelized(String pathPrefix) {
        String filename;
        byte[] md5;
        String md5str;

        filename = path.substring(path.lastIndexOf('/') + 1); // ok when not found
        md5 = md5();
        md5str = Hex.encodeString(md5);
        if (md5str.length() < 3) {
            throw new IllegalArgumentException(md5str);
        }
        return new Label(path, pathPrefix + md5str.substring(0, 3) + "/" + md5str.substring(3) + "/" + folderName + "/" + filename, md5);
    }

    public Label labelNormal(String pathPrefix) {
        return new Label(path, pathPrefix + path, md5());
    }

    //-- utils

    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public byte[] md5() {
        DIGEST.update(data, 0, data.length);
        return DIGEST.digest();
    }

}
