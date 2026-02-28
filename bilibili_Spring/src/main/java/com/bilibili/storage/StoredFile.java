package com.bilibili.storage;

public class StoredFile {

    private final String publicUrl;

    public StoredFile(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}
