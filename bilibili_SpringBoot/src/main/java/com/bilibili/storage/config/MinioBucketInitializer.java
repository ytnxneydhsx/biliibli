package com.bilibili.storage.config;

import com.bilibili.config.properties.MinioProperties;
import com.bilibili.tool.StringTool;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.SetBucketCorsArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.messages.CORSConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;

@Component
public class MinioBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);

    private final MinioAsyncClient minioInternalClient;
    private final MinioProperties minioProperties;

    public MinioBucketInitializer(@Qualifier("minioInternalClient") MinioAsyncClient minioInternalClient,
                                  MinioProperties minioProperties) {
        this.minioInternalClient = minioInternalClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String bucket = minioProperties.getBucket();
        try {
            boolean exists = minioInternalClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            ).join();
            if (!exists) {
                minioInternalClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                ).join();
            }

            minioInternalClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucket)
                            .config(buildPublicReadPolicy(bucket))
                            .build()
            ).join();

            try {
                minioInternalClient.setBucketCors(
                        SetBucketCorsArgs.builder()
                                .bucket(bucket)
                                .config(buildCorsConfiguration())
                                .build()
                ).join();
            } catch (Exception e) {
                log.warn("set MinIO bucket cors failed, continue without bucket cors: bucket={}, cause={}",
                        bucket, unwrap(e).getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("initialize MinIO bucket failed", unwrap(e));
        }
    }

    private CORSConfiguration buildCorsConfiguration() {
        List<String> allowedOrigins = parseCsv(minioProperties.getCorsAllowedOrigins());
        List<CORSConfiguration.CORSRule> rules = new ArrayList<>();
        rules.add(new CORSConfiguration.CORSRule(
                List.of("*"),
                List.of("GET", "PUT", "HEAD"),
                allowedOrigins.isEmpty() ? List.of("*") : allowedOrigins,
                List.of("ETag", "x-amz-request-id", "x-amz-id-2"),
                "bilibili-upload-cors",
                3600
        ));
        return new CORSConfiguration(rules);
    }

    private static List<String> parseCsv(String value) {
        if (StringTool.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static String buildPublicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
