package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageProperties {

    @Value("${storage.rootDir:F:/bilibili-data}")
    private String rootDir;

    @Value("${storage.publicBaseUrl:http://localhost:9000/media}")
    private String publicBaseUrl;

    @Value("${storage.avatarSubDir:avatar}")
    private String avatarSubDir;

    @Value("${storage.coverSubDir:cover}")
    private String coverSubDir;

    @Value("${storage.videoSubDir:video}")
    private String videoSubDir;

    @Value("${storage.avatar.maxSize:2097152}")
    private long avatarMaxSize;

    @Value("${storage.cover.maxSize:5242880}")
    private long coverMaxSize;

    @Value("${storage.video.maxSize:2147483648}")
    private long videoMaxSize;

    @Value("${storage.allowedImageTypes:image/jpeg,image/png,image/webp}")
    private String allowedImageTypes;

    @Value("${storage.allowedVideoTypes:video/mp4}")
    private String allowedVideoTypes;

    public String getRootDir() {
        return rootDir;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public String getAvatarSubDir() {
        return avatarSubDir;
    }

    public String getCoverSubDir() {
        return coverSubDir;
    }

    public String getVideoSubDir() {
        return videoSubDir;
    }

    public long getAvatarMaxSize() {
        return avatarMaxSize;
    }

    public long getCoverMaxSize() {
        return coverMaxSize;
    }

    public long getVideoMaxSize() {
        return videoMaxSize;
    }

    public String getAllowedImageTypes() {
        return allowedImageTypes;
    }

    public String getAllowedVideoTypes() {
        return allowedVideoTypes;
    }
}
