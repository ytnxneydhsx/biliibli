package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinioProperties {

    @Value("${minio.endpoint:http://minio:9000}")
    private String endpoint;

    @Value("${minio.publicEndpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${minio.accessKey:minioadmin}")
    private String accessKey;

    @Value("${minio.secretKey:minioadmin}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    @Value("${minio.bucket:bilibili-media}")
    private String bucket;

    @Value("${minio.avatarPrefix:avatar}")
    private String avatarPrefix;

    @Value("${minio.coverPrefix:cover}")
    private String coverPrefix;

    @Value("${minio.videoPrefix:video}")
    private String videoPrefix;

    @Value("${minio.partUrlExpireSeconds:1800}")
    private int partUrlExpireSeconds;

    @Value("${minio.sessionExpireHours:24}")
    private int sessionExpireHours;

    @Value("${minio.corsAllowedOrigins:http://localhost:63342,http://127.0.0.1:63342,http://localhost:8080,http://127.0.0.1:8080}")
    private String corsAllowedOrigins;

    public String getEndpoint() {
        return endpoint;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getRegion() {
        return region;
    }

    public String getBucket() {
        return bucket;
    }

    public String getAvatarPrefix() {
        return avatarPrefix;
    }

    public String getCoverPrefix() {
        return coverPrefix;
    }

    public String getVideoPrefix() {
        return videoPrefix;
    }

    public int getPartUrlExpireSeconds() {
        return partUrlExpireSeconds;
    }

    public int getSessionExpireHours() {
        return sessionExpireHours;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }
}
