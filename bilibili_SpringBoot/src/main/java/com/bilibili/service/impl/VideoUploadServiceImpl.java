package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilibili.common.exception.ForbiddenException;
import com.bilibili.config.properties.StorageProperties;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.mapper.VideoUploadTaskMapper;
import com.bilibili.model.dto.VideoUploadCompleteDTO;
import com.bilibili.model.dto.VideoUploadInitDTO;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.entity.VideoUploadTaskDO;
import com.bilibili.model.vo.VideoUploadCompleteVO;
import com.bilibili.model.vo.VideoUploadInitVO;
import com.bilibili.model.vo.VideoUploadStatusVO;
import com.bilibili.service.VideoUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoUploadServiceImpl implements VideoUploadService {

    private static final int STATUS_UPLOADING = 0;
    private static final int STATUS_MERGING = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_EXPIRED = 3;
    private static final int STATUS_FAILED = 4;

    private final VideoUploadTaskMapper videoUploadTaskMapper;
    private final VideoMapper videoMapper;
    private final StorageProperties storageProperties;

    @Autowired
    public VideoUploadServiceImpl(VideoUploadTaskMapper videoUploadTaskMapper,
                                  VideoMapper videoMapper,
                                  StorageProperties storageProperties) {
        this.videoUploadTaskMapper = videoUploadTaskMapper;
        this.videoMapper = videoMapper;
        this.storageProperties = storageProperties;
    }

    @Override
    public VideoUploadInitVO initUpload(Long uid, VideoUploadInitDTO dto) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("init upload request is null");
        }

        String fileName = normalizeRequired(dto.getFileName(), "fileName");
        String contentType = normalizeOptional(dto.getContentType());
        long totalSize = requirePositive(dto.getTotalSize(), "totalSize");
        int chunkSize = requirePositive(dto.getChunkSize(), "chunkSize");
        int totalChunks = requirePositive(dto.getTotalChunks(), "totalChunks");

        if (totalSize > storageProperties.getVideoMaxSize()) {
            throw new IllegalArgumentException("video file is too large");
        }
        if (contentType != null && !getAllowedVideoTypes().contains(contentType)) {
            throw new IllegalArgumentException("video content type is not allowed");
        }

        long expectedChunks = (totalSize + chunkSize - 1L) / chunkSize;
        if (expectedChunks != totalChunks) {
            throw new IllegalArgumentException("totalChunks does not match totalSize/chunkSize");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusHours(24);
        String tempRelativeDir = String.format("tmp/%s/%d/%s", storageProperties.getVideoSubDir(), uid, uploadId);

        VideoUploadTaskDO task = new VideoUploadTaskDO();
        task.setUploadId(uploadId);
        task.setUserId(uid);
        task.setFileName(fileName);
        task.setContentType(contentType);
        task.setFileSize(totalSize);
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setStatus(STATUS_UPLOADING);
        task.setTempDir(tempRelativeDir);
        task.setExpireTime(expireTime);

        int rows = videoUploadTaskMapper.insert(task);
        if (rows != 1) {
            throw new RuntimeException("init upload task failed");
        }

        Path tempDir = resolveFromRoot(tempRelativeDir);
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("create upload temp directory failed", e);
        }

        VideoUploadInitVO vo = new VideoUploadInitVO();
        vo.setUploadId(uploadId);
        vo.setChunkSize(chunkSize);
        vo.setTotalChunks(totalChunks);
        vo.setExpireTime(expireTime.toString());
        return vo;
    }

    @Override
    public void uploadChunk(Long uid, String uploadId, Integer index, MultipartFile file) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (index == null || index < 0) {
            throw new IllegalArgumentException("chunk index is invalid");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chunk file is empty");
        }

        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        ensureTaskUploadable(task);

        if (index >= task.getTotalChunks()) {
            throw new IllegalArgumentException("chunk index out of range");
        }

        long expectedChunkSize = expectedChunkSize(task, index);
        if (file.getSize() != expectedChunkSize) {
            throw new IllegalArgumentException("chunk size mismatch");
        }

        Path tempDir = resolveFromRoot(task.getTempDir());
        Path chunkPath = tempDir.resolve(index + ".part").normalize();
        Path tmpChunkPath = tempDir.resolve(index + ".part.tmp").normalize();
        if (!chunkPath.startsWith(tempDir) || !tmpChunkPath.startsWith(tempDir)) {
            throw new IllegalArgumentException("invalid chunk target path");
        }

        try {
            Files.createDirectories(tempDir);
            if (Files.exists(chunkPath) && Files.size(chunkPath) == expectedChunkSize) {
                return;
            }
            Files.deleteIfExists(chunkPath);
            Files.deleteIfExists(tmpChunkPath);
            file.transferTo(tmpChunkPath.toFile());
            Files.move(tmpChunkPath, chunkPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("save chunk failed", e);
        }
    }

    @Override
    public VideoUploadStatusVO getUploadStatus(Long uid, String uploadId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        List<Integer> uploadedChunks = listUploadedChunkIndexes(task);

        VideoUploadStatusVO vo = new VideoUploadStatusVO();
        vo.setUploadId(task.getUploadId());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setUploadedChunks(uploadedChunks);
        vo.setUploadedChunkCount(uploadedChunks.size());
        vo.setCompleted(task.getStatus() != null && task.getStatus() == STATUS_DONE);
        vo.setExpireTime(task.getExpireTime() == null ? null : task.getExpireTime().toString());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoUploadCompleteVO completeUpload(Long uid, String uploadId, VideoUploadCompleteDTO dto) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("complete upload request is null");
        }

        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        if (task.getStatus() != null && task.getStatus() == STATUS_DONE) {
            return buildCompleteVO(task);
        }
        ensureTaskUploadable(task);

        String title = normalizeRequired(dto.getTitle(), "title");
        if (title.length() > 100) {
            throw new IllegalArgumentException("title is too long");
        }
        String description = normalizeOptional(dto.getDescription());
        String coverUrl = normalizeOptional(dto.getCoverUrl());
        long duration = dto.getDuration() == null ? 0L : Math.max(dto.getDuration(), 0L);

        List<Integer> uploadedChunks = listUploadedChunkIndexes(task);
        validateAllChunksPresent(task.getTotalChunks(), uploadedChunks);

        LambdaUpdateWrapper<VideoUploadTaskDO> markMerging = new LambdaUpdateWrapper<>();
        markMerging.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                .eq(VideoUploadTaskDO::getStatus, STATUS_UPLOADING)
                .set(VideoUploadTaskDO::getStatus, STATUS_MERGING);
        int updateRows = videoUploadTaskMapper.update(null, markMerging);
        if (updateRows != 1) {
            VideoUploadTaskDO latest = getOwnedTask(uid, uploadId);
            if (latest.getStatus() != null && latest.getStatus() == STATUS_DONE) {
                return buildCompleteVO(latest);
            }
            throw new IllegalArgumentException("upload task is not in uploading status");
        }

        Path finalPath = null;
        try {
            String relativeVideoPath = buildFinalVideoRelativePath(task.getFileName());
            finalPath = resolveFromRoot(relativeVideoPath);
            Files.createDirectories(finalPath.getParent());
            mergeChunksToFile(task, finalPath);

            String videoUrl = buildPublicUrl(relativeVideoPath);
            VideoDO video = new VideoDO();
            video.setUserId(uid);
            video.setTitle(title);
            video.setDescription(description);
            video.setCoverUrl(coverUrl);
            video.setVideoUrl(videoUrl);
            video.setDuration(duration);
            video.setViewCount(0L);
            video.setLikeCount(0L);
            video.setCommentCount(0L);
            video.setStatus(0);
            int insertVideoRows = videoMapper.insert(video);
            if (insertVideoRows != 1 || video.getId() == null) {
                throw new RuntimeException("insert video failed");
            }

            LambdaUpdateWrapper<VideoUploadTaskDO> markDone = new LambdaUpdateWrapper<>();
            markDone.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                    .eq(VideoUploadTaskDO::getStatus, STATUS_MERGING)
                    .set(VideoUploadTaskDO::getStatus, STATUS_DONE)
                    .set(VideoUploadTaskDO::getFinalVideoId, video.getId())
                    .set(VideoUploadTaskDO::getFinalVideoUrl, videoUrl)
                    .set(VideoUploadTaskDO::getErrorMsg, null);
            int markDoneRows = videoUploadTaskMapper.update(null, markDone);
            if (markDoneRows != 1) {
                throw new RuntimeException("mark upload task done failed");
            }

            cleanupTempDir(task);

            VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
            vo.setUploadId(task.getUploadId());
            vo.setVideoId(video.getId());
            vo.setVideoUrl(videoUrl);
            return vo;
        } catch (Exception e) {
            markTaskFailed(task.getUploadId(), e.getMessage());
            if (finalPath != null) {
                try {
                    Files.deleteIfExists(finalPath);
                } catch (IOException ignore) {
                    // ignore cleanup error
                }
            }
            throw new RuntimeException("complete upload failed", e);
        }
    }

    private VideoUploadTaskDO getOwnedTask(Long uid, String uploadId) {
        String normalizedUploadId = normalizeRequired(uploadId, "uploadId");

        LambdaQueryWrapper<VideoUploadTaskDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoUploadTaskDO::getUploadId, normalizedUploadId);
        VideoUploadTaskDO task = videoUploadTaskMapper.selectOne(query);
        if (task == null) {
            throw new IllegalArgumentException("upload task not found");
        }
        if (!uid.equals(task.getUserId())) {
            throw new ForbiddenException("no permission for this upload task");
        }
        if (task.getExpireTime() != null
                && LocalDateTime.now().isAfter(task.getExpireTime())
                && (task.getStatus() == null || task.getStatus() == STATUS_UPLOADING)) {
            LambdaUpdateWrapper<VideoUploadTaskDO> markExpired = new LambdaUpdateWrapper<>();
            markExpired.eq(VideoUploadTaskDO::getUploadId, normalizedUploadId)
                    .eq(VideoUploadTaskDO::getStatus, STATUS_UPLOADING)
                    .set(VideoUploadTaskDO::getStatus, STATUS_EXPIRED);
            videoUploadTaskMapper.update(null, markExpired);
            throw new IllegalArgumentException("upload task is expired");
        }
        return task;
    }

    private void ensureTaskUploadable(VideoUploadTaskDO task) {
        Integer status = task.getStatus();
        if (status == null || status != STATUS_UPLOADING) {
            throw new IllegalArgumentException("upload task is not uploadable");
        }
    }

    private long expectedChunkSize(VideoUploadTaskDO task, int index) {
        if (index < task.getTotalChunks() - 1) {
            return task.getChunkSize();
        }
        return task.getFileSize() - (long) task.getChunkSize() * (task.getTotalChunks() - 1);
    }

    private List<Integer> listUploadedChunkIndexes(VideoUploadTaskDO task) {
        Path tempDir = resolveFromRoot(task.getTempDir());
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            return Collections.emptyList();
        }

        List<Integer> indexes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, "*.part")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                int dotIndex = name.indexOf('.');
                if (dotIndex <= 0) {
                    continue;
                }
                String prefix = name.substring(0, dotIndex);
                try {
                    indexes.add(Integer.parseInt(prefix));
                } catch (NumberFormatException ignore) {
                    // ignore invalid part file name
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("read uploaded chunks failed", e);
        }
        Collections.sort(indexes);
        return indexes;
    }

    private void validateAllChunksPresent(int totalChunks, List<Integer> uploadedChunks) {
        if (uploadedChunks == null || uploadedChunks.size() != totalChunks) {
            throw new IllegalArgumentException("chunks are not complete");
        }
        Set<Integer> chunkSet = new HashSet<>(uploadedChunks);
        for (int i = 0; i < totalChunks; i++) {
            if (!chunkSet.contains(i)) {
                throw new IllegalArgumentException("chunks are not complete");
            }
        }
    }

    private void mergeChunksToFile(VideoUploadTaskDO task, Path finalPath) {
        Path tempDir = resolveFromRoot(task.getTempDir());
        try (OutputStream outputStream = Files.newOutputStream(
                finalPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (int i = 0; i < task.getTotalChunks(); i++) {
                Path chunkPath = tempDir.resolve(i + ".part").normalize();
                if (!chunkPath.startsWith(tempDir) || !Files.exists(chunkPath)) {
                    throw new IllegalArgumentException("chunks are not complete");
                }
                Files.copy(chunkPath, outputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("merge chunks failed", e);
        }
    }

    private void cleanupTempDir(VideoUploadTaskDO task) {
        Path tempDir = resolveFromRoot(task.getTempDir());
        if (!Files.exists(tempDir)) {
            return;
        }
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignore) {
                            // ignore cleanup exception
                        }
                    });
        } catch (IOException ignore) {
            // ignore cleanup exception
        }
    }

    private void markTaskFailed(String uploadId, String message) {
        String errorMessage = normalizeOptional(message);
        if (errorMessage != null && errorMessage.length() > 255) {
            errorMessage = errorMessage.substring(0, 255);
        }
        LambdaUpdateWrapper<VideoUploadTaskDO> markFailed = new LambdaUpdateWrapper<>();
        markFailed.eq(VideoUploadTaskDO::getUploadId, uploadId)
                .eq(VideoUploadTaskDO::getStatus, STATUS_MERGING)
                .set(VideoUploadTaskDO::getStatus, STATUS_FAILED)
                .set(VideoUploadTaskDO::getErrorMsg, errorMessage);
        videoUploadTaskMapper.update(null, markFailed);
    }

    private String buildFinalVideoRelativePath(String originalFileName) {
        LocalDate today = LocalDate.now();
        String extension = resolveVideoExtension(originalFileName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        return String.format("%s/%d/%02d/%02d/%s",
                storageProperties.getVideoSubDir(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                fileName);
    }

    private String resolveVideoExtension(String originalFileName) {
        if (originalFileName != null) {
            int index = originalFileName.lastIndexOf('.');
            if (index > 0 && index < originalFileName.length() - 1) {
                String ext = originalFileName.substring(index).toLowerCase();
                if (ext.matches("\\.[a-z0-9]{1,10}")) {
                    return ext;
                }
            }
        }
        return ".mp4";
    }

    private Path resolveFromRoot(String relativePath) {
        Path root = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid storage path");
        }
        return resolved;
    }

    private String buildPublicUrl(String relativePath) {
        String baseUrl = trimTrailingSlash(storageProperties.getPublicBaseUrl());
        String normalizedRelativePath = relativePath.replace("\\", "/").replaceFirst("^/+", "");
        return baseUrl + "/" + normalizedRelativePath;
    }

    private List<String> getAllowedVideoTypes() {
        String csv = storageProperties.getAllowedVideoTypes();
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static VideoUploadCompleteVO buildCompleteVO(VideoUploadTaskDO task) {
        VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
        vo.setUploadId(task.getUploadId());
        vo.setVideoId(task.getFinalVideoId());
        vo.setVideoUrl(task.getFinalVideoUrl());
        return vo;
    }
}
