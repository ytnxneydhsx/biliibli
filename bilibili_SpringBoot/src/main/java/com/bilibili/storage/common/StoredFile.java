package com.bilibili.storage.common;

public class StoredFile {

    private final String publicUrl;

    public StoredFile(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}
