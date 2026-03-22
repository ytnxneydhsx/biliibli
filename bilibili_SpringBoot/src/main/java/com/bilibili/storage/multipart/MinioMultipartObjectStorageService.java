package com.bilibili.storage.multipart;

import com.bilibili.config.properties.MinioProperties;
import com.bilibili.config.properties.StorageProperties;
import com.bilibili.tool.StringTool;
import com.bilibili.upload.video.model.dto.VideoUploadPartETagDTO;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.minio.CreateMultipartUploadResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListPartsResponse;
import io.minio.MinioAsyncClient;
import io.minio.RemoveObjectArgs;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Service
public class MinioMultipartObjectStorageService implements MultipartObjectStorageService {

    private final MinioAsyncClient minioInternalClient;
    private final MinioAsyncClient minioPresignClient;
    private final MinioProperties minioProperties;
    private final StorageProperties storageProperties;

    public MinioMultipartObjectStorageService(@Qualifier("minioInternalClient") MinioAsyncClient minioInternalClient,
                                              @Qualifier("minioPresignClient") MinioAsyncClient minioPresignClient,
                                              MinioProperties minioProperties,
                                              StorageProperties storageProperties) {
        this.minioInternalClient = minioInternalClient;
        this.minioPresignClient = minioPresignClient;
        this.minioProperties = minioProperties;
        this.storageProperties = storageProperties;
    }

    @Override
    public long getMaxObjectSize() {
        return storageProperties.getVideoMaxSize();
    }

    @Override
    public int getChunkSize() {
        return Math.max(storageProperties.getVideoChunkSize(), 5 * 1024 * 1024);
    }

    @Override
    public boolean isAllowedContentType(String contentType) {
        String normalizedContentType = StringTool.normalizeOptional(contentType);
        if (normalizedContentType == null) {
            return false;
        }
        return Arrays.stream(storageProperties.getAllowedVideoTypes().split(","))
                .map(String::trim)
                .anyMatch(normalizedContentType::equalsIgnoreCase);
    }

    @Override
    public boolean isAllowedFileName(String originalFileName) {
        String extension = resolveOriginalExtension(originalFileName);
        if (extension == null) {
            return false;
        }
        return Arrays.stream(storageProperties.getAllowedVideoExtensions().split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .anyMatch(extension::equalsIgnoreCase);
    }

    @Override
    public String buildObjectKey(Long uid, String originalFileName, String contentType) {
        LocalDate today = LocalDate.now();
        String extension = resolveVideoExtension(originalFileName, contentType);
        return "%s/%d/%d/%02d/%02d/%s%s".formatted(
                trimSlashes(minioProperties.getVideoPrefix()),
                uid,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );
    }

    @Override
    public String buildPublicUrl(String objectKey) {
        String base = StringTool.trimTrailingSlash(minioProperties.getPublicEndpoint());
        String normalizedObjectKey = trimLeadingSlash(objectKey);
        return base + "/" + minioProperties.getBucket() + "/" + normalizedObjectKey;
    }

    @Override
    public String createMultipartUpload(String objectKey, String contentType) {
        try {
            Multimap<String, String> headers = null;
            String normalizedContentType = StringTool.normalizeOptional(contentType);
            if (normalizedContentType != null) {
                headers = LinkedListMultimap.create();
                headers.put("Content-Type", normalizedContentType);
            }
            CreateMultipartUploadResponse response = minioInternalClient.createMultipartUploadAsync(
                    minioProperties.getBucket(),
                    minioProperties.getRegion(),
                    objectKey,
                    headers,
                    null
            ).join();
            return response.result().uploadId();
        } catch (CompletionException e) {
            throw new RuntimeException("create multipart upload failed", unwrap(e));
        } catch (Exception e) {
            throw new RuntimeException("create multipart upload failed", e);
        }
    }

    @Override
    public Map<Integer, String> signUploadPartUrls(String objectKey, String multipartUploadId, List<Integer> partNumbers) {
        Map<Integer, String> urls = new LinkedHashMap<>();
        for (Integer partNumber : partNumbers) {
            Map<String, String> queryParams = new LinkedHashMap<>();
            queryParams.put("partNumber", String.valueOf(partNumber));
            queryParams.put("uploadId", multipartUploadId);
            try {
                String url = minioPresignClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(minioProperties.getBucket())
                                .region(minioProperties.getRegion())
                                .object(objectKey)
                                .expiry(minioProperties.getPartUrlExpireSeconds())
                                .extraQueryParams(queryParams)
                                .build()
                );
                urls.put(partNumber, url);
            } catch (Exception e) {
                throw new RuntimeException("sign upload part url failed", e);
            }
        }
        return urls;
    }

    @Override
    public List<Integer> listUploadedParts(String objectKey, String multipartUploadId) {
        if (StringTool.isBlank(objectKey) || StringTool.isBlank(multipartUploadId)) {
            return List.of();
        }
        try {
            ListPartsResponse response = minioInternalClient.listPartsAsync(
                    minioProperties.getBucket(),
                    minioProperties.getRegion(),
                    objectKey,
                    1000,
                    0,
                    multipartUploadId,
                    null,
                    null
            ).join();
            ListPartsResult result = response.result();
            if (result == null || result.partList() == null) {
                return List.of();
            }
            return result.partList().stream()
                    .map(Part::partNumber)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void completeMultipartUpload(String objectKey, String multipartUploadId, List<VideoUploadPartETagDTO> parts) {
        Part[] minioParts = parts.stream()
                .sorted(Comparator.comparing(VideoUploadPartETagDTO::getPartNumber))
                .map(part -> new Part(part.getPartNumber(), part.getEtag()))
                .toArray(Part[]::new);
        try {
            minioInternalClient.completeMultipartUploadAsync(
                    minioProperties.getBucket(),
                    minioProperties.getRegion(),
                    objectKey,
                    multipartUploadId,
                    minioParts,
                    null,
                    null
            ).join();
        } catch (CompletionException e) {
            throw new RuntimeException("complete multipart upload failed", unwrap(e));
        } catch (Exception e) {
            throw new RuntimeException("complete multipart upload failed", e);
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String multipartUploadId) {
        if (StringTool.isBlank(multipartUploadId)) {
            return;
        }
        try {
            minioInternalClient.abortMultipartUploadAsync(
                    minioProperties.getBucket(),
                    minioProperties.getRegion(),
                    objectKey,
                    multipartUploadId,
                    null,
                    null
            ).join();
        } catch (CompletionException ignore) {
            // Ignore cleanup errors after a failed or cancelled upload.
        } catch (Exception ignore) {
            // Ignore cleanup errors after a failed or cancelled upload.
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            minioInternalClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .region(minioProperties.getRegion())
                            .object(objectKey)
                            .build()
            ).join();
        } catch (CompletionException ignore) {
            // Ignore cleanup errors after a failed finalize step.
        } catch (Exception ignore) {
            // Ignore cleanup errors after a failed finalize step.
        }
    }

    private static String resolveVideoExtension(String originalFileName, String contentType) {
        String extension = resolveOriginalExtension(originalFileName);
        if (extension != null) {
            return extension;
        }
        if ("video/quicktime".equalsIgnoreCase(contentType)) {
            return ".mov";
        }
        if ("video/webm".equalsIgnoreCase(contentType)) {
            return ".webm";
        }
        if ("video/x-m4v".equalsIgnoreCase(contentType)) {
            return ".m4v";
        }
        if ("video/x-matroska".equalsIgnoreCase(contentType)) {
            return ".mkv";
        }
        if ("video/ogg".equalsIgnoreCase(contentType)) {
            return ".ogv";
        }
        return ".mp4";
    }

    private static String resolveOriginalExtension(String originalFileName) {
        if (StringTool.isBlank(originalFileName)) {
            return null;
        }
        int index = originalFileName.lastIndexOf('.');
        if (index < 0 || index >= originalFileName.length() - 1) {
            return null;
        }
        return originalFileName.substring(index).toLowerCase();
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
