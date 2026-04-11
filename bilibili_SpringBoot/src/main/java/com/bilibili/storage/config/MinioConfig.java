package com.bilibili.storage.config;

import com.bilibili.config.properties.MinioProperties;
import io.minio.MinioAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean("minioInternalClient")
    public MinioAsyncClient minioInternalClient(MinioProperties minioProperties, Environment environment) {
        log.info("minio env raw MINIO_ACCESS_KEY={} MINIO_SECRET_KEY={} minio.accessKey={} minio.secretKey={}",
                maskAccessKey(System.getenv("MINIO_ACCESS_KEY")),
                maskAccessKey(System.getenv("MINIO_SECRET_KEY")),
                maskAccessKey(environment.getProperty("minio.accessKey")),
                maskAccessKey(environment.getProperty("minio.secretKey")));
        log.info("minio env variants minio.access-key={} minio.accessKey={} minio.secret-key={} minio.secretKey={}",
                maskAccessKey(environment.getProperty("minio.access-key")),
                maskAccessKey(environment.getProperty("minio.accessKey")),
                maskAccessKey(environment.getProperty("minio.secret-key")),
                maskAccessKey(environment.getProperty("minio.secretKey")));
        log.info("create minioInternalClient endpoint={} accessKey={}",
                minioProperties.getEndpoint(),
                maskAccessKey(minioProperties.getAccessKey()));
        return MinioAsyncClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .region(minioProperties.getRegion())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean("minioPresignClient")
    public MinioAsyncClient minioPresignClient(MinioProperties minioProperties, Environment environment) {
        log.info("minio presign env minio.publicEndpoint={} minio.accessKey={}",
                environment.getProperty("minio.publicEndpoint"),
                maskAccessKey(environment.getProperty("minio.accessKey")));
        log.info("create minioPresignClient endpoint={} accessKey={}",
                minioProperties.getPublicEndpoint(),
                maskAccessKey(minioProperties.getAccessKey()));
        return MinioAsyncClient.builder()
                .endpoint(minioProperties.getPublicEndpoint())
                .region(minioProperties.getRegion())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    private static String maskAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            return "<blank>";
        }
        if (accessKey.length() <= 4) {
            return "****";
        }
        return accessKey.substring(0, 2) + "****" + accessKey.substring(accessKey.length() - 2);
    }
}
