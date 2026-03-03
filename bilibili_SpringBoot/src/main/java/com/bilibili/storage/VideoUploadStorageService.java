package com.bilibili.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoUploadStorageService {

    long getMaxVideoSize();

    boolean isAllowedVideoType(String contentType);

    String buildTempRelativeDir(Long uid, String uploadId);

    String buildFinalVideoRelativePath(String originalFileName);

    String buildPublicUrl(String relativePath);

    void createDirectory(String relativeDir);

    void saveChunk(String tempRelativeDir, int index, MultipartFile file, long expectedChunkSize);

    List<Integer> listUploadedChunkIndexes(String tempRelativeDir);

    void mergeChunks(String tempRelativeDir, int totalChunks, String finalRelativePath);

    void cleanupTempDir(String tempRelativeDir);

    void deleteByRelativePath(String relativePath);
}

