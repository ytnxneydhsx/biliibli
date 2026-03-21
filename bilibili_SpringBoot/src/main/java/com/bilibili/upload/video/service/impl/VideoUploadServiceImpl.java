package com.bilibili.upload.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.enums.UploadTaskStatus;
import com.bilibili.common.exception.ForbiddenException;
import com.bilibili.config.properties.MinioProperties;
import com.bilibili.storage.multipart.MultipartObjectStorageService;
import com.bilibili.tool.StringTool;
import com.bilibili.upload.video.mapper.VideoUploadTaskMapper;
import com.bilibili.upload.video.model.dto.VideoUploadCompleteDTO;
import com.bilibili.upload.video.model.dto.VideoUploadInitDTO;
import com.bilibili.upload.video.model.dto.VideoUploadPartETagDTO;
import com.bilibili.upload.video.model.dto.VideoUploadPartSignDTO;
import com.bilibili.upload.video.model.entity.VideoUploadTaskDO;
import com.bilibili.upload.video.model.vo.VideoUploadCompleteVO;
import com.bilibili.upload.video.model.vo.VideoUploadInitVO;
import com.bilibili.upload.video.model.vo.VideoUploadPartSignVO;
import com.bilibili.upload.video.model.vo.VideoUploadSignedPartVO;
import com.bilibili.upload.video.model.vo.VideoUploadStatusVO;
import com.bilibili.upload.video.service.VideoUploadService;
import com.bilibili.video.mapper.VideoMapper;
import com.bilibili.video.model.entity.VideoDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoUploadServiceImpl implements VideoUploadService {

    private final VideoUploadTaskMapper videoUploadTaskMapper;
    private final VideoMapper videoMapper;
    private final MultipartObjectStorageService multipartObjectStorageService;
    private final MinioProperties minioProperties;

    public VideoUploadServiceImpl(VideoUploadTaskMapper videoUploadTaskMapper,
                                  VideoMapper videoMapper,
                                  MultipartObjectStorageService multipartObjectStorageService,
                                  MinioProperties minioProperties) {
        this.videoUploadTaskMapper = videoUploadTaskMapper;
        this.videoMapper = videoMapper;
        this.multipartObjectStorageService = multipartObjectStorageService;
        this.minioProperties = minioProperties;
    }

    @Override
    public VideoUploadInitVO initUpload(Long uid, VideoUploadInitDTO dto) {
        requireValidUid(uid);
        if (dto == null) {
            throw new IllegalArgumentException("init upload request is null");
        }

        String fileName = StringTool.normalizeRequired(dto.getFileName(), "fileName");
        String contentType = StringTool.normalizeOptional(dto.getContentType());
        long totalSize = requirePositive(dto.getTotalSize(), "totalSize");
        int chunkSize = multipartObjectStorageService.getChunkSize();
        int totalChunks = (int) ((totalSize + chunkSize - 1L) / chunkSize);

        if (totalSize > multipartObjectStorageService.getMaxObjectSize()) {
            throw new IllegalArgumentException("video file is too large");
        }
        if (!multipartObjectStorageService.isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("video content type is not allowed");
        }
        if (!multipartObjectStorageService.isAllowedFileName(fileName)) {
            throw new IllegalArgumentException("video file extension is not allowed");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String objectKey = multipartObjectStorageService.buildObjectKey(uid, fileName, contentType);
        String multipartUploadId = multipartObjectStorageService.createMultipartUpload(objectKey, contentType);
        LocalDateTime expireTime = LocalDateTime.now().plusHours(minioProperties.getSessionExpireHours());

        VideoUploadTaskDO task = new VideoUploadTaskDO();
        task.setUploadId(uploadId);
        task.setUserId(uid);
        task.setFileName(fileName);
        task.setContentType(contentType);
        task.setFileSize(totalSize);
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setStatus(UploadTaskStatus.UPLOADING.code());
        task.setObjectKey(objectKey);
        task.setMultipartUploadId(multipartUploadId);
        task.setExpireTime(expireTime);

        if (videoUploadTaskMapper.insert(task) != 1) {
            multipartObjectStorageService.abortMultipartUpload(objectKey, multipartUploadId);
            throw new RuntimeException("init upload task failed");
        }

        VideoUploadInitVO vo = new VideoUploadInitVO();
        vo.setUploadId(uploadId);
        vo.setChunkSize(chunkSize);
        vo.setTotalChunks(totalChunks);
        vo.setObjectKey(objectKey);
        vo.setExpireTime(expireTime.toString());
        return vo;
    }

    @Override
    public VideoUploadPartSignVO signUploadParts(Long uid, String uploadId, VideoUploadPartSignDTO dto) {
        requireValidUid(uid);
        if (dto == null || dto.getPartNumbers() == null || dto.getPartNumbers().isEmpty()) {
            throw new IllegalArgumentException("partNumbers is required");
        }

        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        ensureTaskUploadable(task);

        validatePartNumbers(dto.getPartNumbers(), task.getTotalChunks());
        Map<Integer, String> signedUrls = multipartObjectStorageService.signUploadPartUrls(
                task.getObjectKey(),
                task.getMultipartUploadId(),
                dto.getPartNumbers()
        );

        VideoUploadPartSignVO vo = new VideoUploadPartSignVO();
        vo.setUploadId(task.getUploadId());
        vo.setExpireTime(LocalDateTime.now().plusSeconds(minioProperties.getPartUrlExpireSeconds()).toString());
        vo.setParts(signedUrls.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    VideoUploadSignedPartVO part = new VideoUploadSignedPartVO();
                    part.setPartNumber(entry.getKey());
                    part.setUploadUrl(entry.getValue());
                    return part;
                })
                .toList());
        return vo;
    }

    @Override
    public VideoUploadStatusVO getUploadStatus(Long uid, String uploadId) {
        requireValidUid(uid);
        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        List<Integer> uploadedParts = resolveUploadedParts(task);

        VideoUploadStatusVO vo = new VideoUploadStatusVO();
        vo.setUploadId(task.getUploadId());
        vo.setObjectKey(task.getObjectKey());
        vo.setChunkSize(task.getChunkSize());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setUploadedPartCount(uploadedParts.size());
        vo.setUploadedParts(uploadedParts);
        vo.setStatus(task.getStatus());
        vo.setCompleted(UploadTaskStatus.DONE.matches(task.getStatus()));
        vo.setExpireTime(task.getExpireTime() == null ? null : task.getExpireTime().toString());
        vo.setVideoId(task.getFinalVideoId());
        vo.setVideoUrl(task.getFinalVideoUrl());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoUploadCompleteVO completeUpload(Long uid, String uploadId, VideoUploadCompleteDTO dto) {
        requireValidUid(uid);
        if (dto == null) {
            throw new IllegalArgumentException("complete upload request is null");
        }

        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        if (UploadTaskStatus.DONE.matches(task.getStatus())) {
            return buildCompleteVO(task);
        }
        ensureTaskUploadable(task);

        String title = StringTool.normalizeRequired(dto.getTitle(), "title");
        if (title.length() > 100) {
            throw new IllegalArgumentException("title is too long");
        }
        String description = StringTool.normalizeOptional(dto.getDescription());
        String coverUrl = StringTool.normalizeOptional(dto.getCoverUrl());
        long duration = dto.getDuration() == null ? 0L : Math.max(dto.getDuration(), 0L);
        validateCompletedParts(dto.getParts(), task.getTotalChunks());

        LambdaUpdateWrapper<VideoUploadTaskDO> markCompleting = new LambdaUpdateWrapper<>();
        markCompleting.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.UPLOADING.code())
                .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.COMPLETING.code())
                .set(VideoUploadTaskDO::getErrorMsg, null);
        int updateRows = videoUploadTaskMapper.update(null, markCompleting);
        if (updateRows != 1) {
            VideoUploadTaskDO latest = getOwnedTask(uid, uploadId);
            if (UploadTaskStatus.DONE.matches(latest.getStatus())) {
                return buildCompleteVO(latest);
            }
            throw new IllegalArgumentException("upload task is not in uploading status");
        }

        boolean multipartCompleted = false;
        try {
            multipartObjectStorageService.completeMultipartUpload(
                    task.getObjectKey(),
                    task.getMultipartUploadId(),
                    dto.getParts()
            );
            multipartCompleted = true;

            String videoUrl = multipartObjectStorageService.buildPublicUrl(task.getObjectKey());
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
            video.setStatus(RecordStatus.NORMAL.code());
            if (videoMapper.insert(video) != 1 || video.getId() == null) {
                throw new RuntimeException("insert video failed");
            }

            LambdaUpdateWrapper<VideoUploadTaskDO> markDone = new LambdaUpdateWrapper<>();
            markDone.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                    .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.COMPLETING.code())
                    .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.DONE.code())
                    .set(VideoUploadTaskDO::getFinalVideoId, video.getId())
                    .set(VideoUploadTaskDO::getFinalVideoUrl, videoUrl)
                    .set(VideoUploadTaskDO::getErrorMsg, null);
            if (videoUploadTaskMapper.update(null, markDone) != 1) {
                throw new RuntimeException("mark upload task done failed");
            }

            VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
            vo.setUploadId(task.getUploadId());
            vo.setVideoId(video.getId());
            vo.setVideoUrl(videoUrl);
            return vo;
        } catch (Exception e) {
            markTaskFailed(task.getUploadId(), e.getMessage());
            if (multipartCompleted) {
                multipartObjectStorageService.deleteObject(task.getObjectKey());
            } else {
                multipartObjectStorageService.abortMultipartUpload(task.getObjectKey(), task.getMultipartUploadId());
            }
            throw new RuntimeException("complete upload failed", e);
        }
    }

    @Override
    public void cancelUpload(Long uid, String uploadId) {
        requireValidUid(uid);
        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        if (UploadTaskStatus.DONE.matches(task.getStatus()) || UploadTaskStatus.CANCELLED.matches(task.getStatus())) {
            return;
        }

        multipartObjectStorageService.abortMultipartUpload(task.getObjectKey(), task.getMultipartUploadId());
        LambdaUpdateWrapper<VideoUploadTaskDO> cancel = new LambdaUpdateWrapper<>();
        cancel.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                .in(VideoUploadTaskDO::getStatus,
                        UploadTaskStatus.UPLOADING.code(),
                        UploadTaskStatus.COMPLETING.code(),
                        UploadTaskStatus.FAILED.code())
                .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.CANCELLED.code())
                .set(VideoUploadTaskDO::getErrorMsg, "upload cancelled");
        videoUploadTaskMapper.update(null, cancel);
    }

    private VideoUploadTaskDO getOwnedTask(Long uid, String uploadId) {
        String normalizedUploadId = StringTool.normalizeRequired(uploadId, "uploadId");

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
                && UploadTaskStatus.UPLOADING.matches(task.getStatus())) {
            multipartObjectStorageService.abortMultipartUpload(task.getObjectKey(), task.getMultipartUploadId());
            LambdaUpdateWrapper<VideoUploadTaskDO> markExpired = new LambdaUpdateWrapper<>();
            markExpired.eq(VideoUploadTaskDO::getUploadId, normalizedUploadId)
                    .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.UPLOADING.code())
                    .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.EXPIRED.code())
                    .set(VideoUploadTaskDO::getErrorMsg, "upload task expired");
            videoUploadTaskMapper.update(null, markExpired);
            throw new IllegalArgumentException("upload task is expired");
        }
        return task;
    }

    private void ensureTaskUploadable(VideoUploadTaskDO task) {
        if (!UploadTaskStatus.UPLOADING.matches(task.getStatus())) {
            throw new IllegalArgumentException("upload task is not uploadable");
        }
    }

    private List<Integer> resolveUploadedParts(VideoUploadTaskDO task) {
        if (UploadTaskStatus.DONE.matches(task.getStatus())) {
            return buildAllPartNumbers(task.getTotalChunks());
        }
        if (UploadTaskStatus.UPLOADING.matches(task.getStatus())
                || UploadTaskStatus.COMPLETING.matches(task.getStatus())) {
            return multipartObjectStorageService.listUploadedParts(task.getObjectKey(), task.getMultipartUploadId());
        }
        return List.of();
    }

    private List<Integer> buildAllPartNumbers(Integer totalChunks) {
        if (totalChunks == null || totalChunks <= 0) {
            return List.of();
        }
        java.util.ArrayList<Integer> partNumbers = new java.util.ArrayList<>(totalChunks);
        for (int partNumber = 1; partNumber <= totalChunks; partNumber++) {
            partNumbers.add(partNumber);
        }
        return partNumbers;
    }

    private void validatePartNumbers(List<Integer> partNumbers, int totalChunks) {
        Set<Integer> uniquePartNumbers = new HashSet<>();
        for (Integer partNumber : partNumbers) {
            if (partNumber == null || partNumber < 1 || partNumber > totalChunks) {
                throw new IllegalArgumentException("partNumber is out of range");
            }
            if (!uniquePartNumbers.add(partNumber)) {
                throw new IllegalArgumentException("duplicate partNumber is not allowed");
            }
        }
    }

    private void validateCompletedParts(List<VideoUploadPartETagDTO> parts, int totalChunks) {
        if (parts == null || parts.size() != totalChunks) {
            throw new IllegalArgumentException("parts are not complete");
        }
        Set<Integer> partNumbers = new HashSet<>();
        for (VideoUploadPartETagDTO part : parts) {
            if (part == null || part.getPartNumber() == null || StringTool.isBlank(part.getEtag())) {
                throw new IllegalArgumentException("partNumber and etag are required");
            }
            if (part.getPartNumber() < 1 || part.getPartNumber() > totalChunks) {
                throw new IllegalArgumentException("partNumber is out of range");
            }
            if (!partNumbers.add(part.getPartNumber())) {
                throw new IllegalArgumentException("duplicate partNumber is not allowed");
            }
        }
        for (int partNumber = 1; partNumber <= totalChunks; partNumber++) {
            if (!partNumbers.contains(partNumber)) {
                throw new IllegalArgumentException("parts are not complete");
            }
        }
    }

    private void requireValidUid(Long uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
    }

    private void markTaskFailed(String uploadId, String message) {
        String errorMessage = StringTool.normalizeOptional(message);
        if (errorMessage != null && errorMessage.length() > 255) {
            errorMessage = errorMessage.substring(0, 255);
        }
        LambdaUpdateWrapper<VideoUploadTaskDO> markFailed = new LambdaUpdateWrapper<>();
        markFailed.eq(VideoUploadTaskDO::getUploadId, uploadId)
                .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.COMPLETING.code())
                .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.FAILED.code())
                .set(VideoUploadTaskDO::getErrorMsg, errorMessage);
        videoUploadTaskMapper.update(null, markFailed);
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

    private static VideoUploadCompleteVO buildCompleteVO(VideoUploadTaskDO task) {
        VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
        vo.setUploadId(task.getUploadId());
        vo.setVideoId(task.getFinalVideoId());
        vo.setVideoUrl(task.getFinalVideoUrl());
        return vo;
    }
}
