package com.bilibili.storage.image;

import com.bilibili.config.properties.MinioProperties;
import com.bilibili.config.properties.StorageProperties;
import com.bilibili.storage.common.StoredFile;
import com.bilibili.tool.StringTool;
import io.minio.MinioAsyncClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Service
public class MinioImageStorageService implements ImageStorageService {

    private final MinioAsyncClient minioInternalClient;
    private final MinioProperties minioProperties;
    private final StorageProperties storageProperties;

    public MinioImageStorageService(@Qualifier("minioInternalClient") MinioAsyncClient minioInternalClient,
                                    MinioProperties minioProperties,
                                    StorageProperties storageProperties) {
        this.minioInternalClient = minioInternalClient;
        this.minioProperties = minioProperties;
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredFile saveImage(MultipartFile file, ImageStorageType imageStorageType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file is empty");
        }
        if (imageStorageType == null) {
            throw new IllegalArgumentException("image storage type is required");
        }
        if (file.getSize() > resolveImageMaxSize(imageStorageType)) {
            throw new IllegalArgumentException("image file is too large");
        }

        String contentType = StringTool.normalizeOptional(file.getContentType());
        if (contentType == null || !getAllowedImageTypes().contains(contentType)) {
            throw new IllegalArgumentException("image content type is not allowed");
        }

        String objectKey = buildImageObjectKey(imageStorageType, file.getOriginalFilename(), contentType);
        try (InputStream inputStream = file.getInputStream()) {
            minioInternalClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .region(minioProperties.getRegion())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1L)
                            .contentType(contentType)
                            .build()
            ).join();
            return new StoredFile(buildPublicUrl(objectKey));
        } catch (CompletionException e) {
            throw new RuntimeException("save image file failed", unwrap(e));
        } catch (Exception e) {
            throw new RuntimeException("save image file failed", e);
        }
    }

    @Override
    public void deleteByPublicUrl(String publicUrl) {
        String objectKey = extractObjectKey(publicUrl);
        if (objectKey == null) {
            return;
        }
        try {
            minioInternalClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .region(minioProperties.getRegion())
                            .object(objectKey)
                            .build()
            ).join();
        } catch (CompletionException ignore) {
            // Ignore cleanup errors for stale or already removed objects.
        }
    }

    private String buildImageObjectKey(ImageStorageType imageStorageType, String originalFilename, String contentType) {
        LocalDate today = LocalDate.now();
        String extension = resolveImageExtension(originalFilename, contentType);
        return "%s/%d/%02d/%02d/%s%s".formatted(
                trimSlashes(resolveImagePrefix(imageStorageType)),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );
    }

    private String buildPublicUrl(String objectKey) {
        String base = StringTool.trimTrailingSlash(minioProperties.getPublicEndpoint());
        return base + "/" + minioProperties.getBucket() + "/" + trimLeadingSlash(objectKey);
    }

    private String extractObjectKey(String publicUrl) {
        String normalizedUrl = StringTool.normalizeOptional(publicUrl);
        String base = StringTool.trimTrailingSlash(minioProperties.getPublicEndpoint());
        if (normalizedUrl == null || base == null) {
            return null;
        }
        String bucketBase = base + "/" + minioProperties.getBucket();
        if (!normalizedUrl.startsWith(bucketBase + "/")) {
            return null;
        }
        String objectKey = normalizedUrl.substring(bucketBase.length()).replaceFirst("^/+", "");
        return objectKey.isEmpty() ? null : objectKey;
    }

    private long resolveImageMaxSize(ImageStorageType imageStorageType) {
        return switch (imageStorageType) {
            case AVATAR -> storageProperties.getAvatarMaxSize();
            case VIDEO_COVER -> storageProperties.getCoverMaxSize();
        };
    }

    private String resolveImagePrefix(ImageStorageType imageStorageType) {
        return switch (imageStorageType) {
            case AVATAR -> minioProperties.getAvatarPrefix();
            case VIDEO_COVER -> minioProperties.getCoverPrefix();
        };
    }

    private List<String> getAllowedImageTypes() {
        String csv = storageProperties.getAllowedImageTypes();
        if (StringTool.isBlank(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static String resolveImageExtension(String originalFilename, String contentType) {
        if (!StringTool.isBlank(originalFilename)) {
            int index = originalFilename.lastIndexOf('.');
            if (index >= 0 && index < originalFilename.length() - 1) {
                String ext = originalFilename.substring(index).toLowerCase();
                if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext)) {
                    return ext;
                }
            }
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private static String trimLeadingSlash(String value) {
        String normalized = value == null ? "" : value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String trimSlashes(String value) {
        String normalized = trimLeadingSlash(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
