package com.bilibili.storage.config;

import com.bilibili.config.properties.MinioProperties;
import io.minio.MinioAsyncClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean("minioInternalClient")
    public MinioAsyncClient minioInternalClient(MinioProperties minioProperties) {
        return MinioAsyncClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .region(minioProperties.getRegion())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean("minioPresignClient")
    public MinioAsyncClient minioPresignClient(MinioProperties minioProperties) {
        return MinioAsyncClient.builder()
                .endpoint(minioProperties.getPublicEndpoint())
                .region(minioProperties.getRegion())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
