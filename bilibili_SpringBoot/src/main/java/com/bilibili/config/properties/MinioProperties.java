package com.bilibili.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "minio")
@Component
public class MinioProperties {

    private String endpoint = "http://minio:9000";
    private String publicEndpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String region = "us-east-1";
    private String bucket = "bilibili-media";
    private String avatarPrefix = "avatar";
    private String groupAvatarPrefix = "group-avatar";
    private String coverPrefix = "cover";
    private String imImagePrefix = "im";
    private String videoPrefix = "video";
    private int partUrlExpireSeconds = 1800;
    private int sessionExpireHours = 24;
    private String corsAllowedOrigins = "http://localhost:63342,http://127.0.0.1:63342,http://localhost:8080,http://127.0.0.1:8080";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAvatarPrefix() {
        return avatarPrefix;
    }

    public void setAvatarPrefix(String avatarPrefix) {
        this.avatarPrefix = avatarPrefix;
    }

    public String getGroupAvatarPrefix() {
        return groupAvatarPrefix;
    }

    public void setGroupAvatarPrefix(String groupAvatarPrefix) {
        this.groupAvatarPrefix = groupAvatarPrefix;
    }

    public String getCoverPrefix() {
        return coverPrefix;
    }

    public void setCoverPrefix(String coverPrefix) {
        this.coverPrefix = coverPrefix;
    }

    public String getImImagePrefix() {
        return imImagePrefix;
    }

    public void setImImagePrefix(String imImagePrefix) {
        this.imImagePrefix = imImagePrefix;
    }

    public String getVideoPrefix() {
        return videoPrefix;
    }

    public void setVideoPrefix(String videoPrefix) {
        this.videoPrefix = videoPrefix;
    }

    public int getPartUrlExpireSeconds() {
        return partUrlExpireSeconds;
    }

    public void setPartUrlExpireSeconds(int partUrlExpireSeconds) {
        this.partUrlExpireSeconds = partUrlExpireSeconds;
    }

    public int getSessionExpireHours() {
        return sessionExpireHours;
    }

    public void setSessionExpireHours(int sessionExpireHours) {
        this.sessionExpireHours = sessionExpireHours;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }
}
