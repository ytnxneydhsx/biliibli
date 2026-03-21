package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageProperties {

    @Value("${storage.avatar.maxSize:2097152}")
    private long avatarMaxSize;

    @Value("${storage.cover.maxSize:5242880}")
    private long coverMaxSize;

    @Value("${storage.video.maxSize:2147483648}")
    private long videoMaxSize;

    @Value("${storage.video.chunkSize:10485760}")
    private int videoChunkSize;

    @Value("${storage.allowedImageTypes:image/jpeg,image/png,image/webp}")
    private String allowedImageTypes;

    @Value("${storage.allowedVideoTypes:video/mp4}")
    private String allowedVideoTypes;

    @Value("${storage.allowedVideoExtensions:.mp4,.mov,.webm,.m4v,.mkv,.ogv}")
    private String allowedVideoExtensions;

    public long getAvatarMaxSize() {
        return avatarMaxSize;
    }

    public long getCoverMaxSize() {
        return coverMaxSize;
    }

    public long getVideoMaxSize() {
        return videoMaxSize;
    }

    public int getVideoChunkSize() {
        return videoChunkSize;
    }

    public String getAllowedImageTypes() {
        return allowedImageTypes;
    }

    public String getAllowedVideoTypes() {
        return allowedVideoTypes;
    }

    public String getAllowedVideoExtensions() {
        return allowedVideoExtensions;
    }
}
