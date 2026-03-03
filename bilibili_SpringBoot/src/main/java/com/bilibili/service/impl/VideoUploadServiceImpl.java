package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilibili.common.exception.ForbiddenException;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.enums.UploadTaskStatus;
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
import com.bilibili.storage.VideoUploadStorageService;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoUploadServiceImpl implements VideoUploadService {

    private final VideoUploadTaskMapper videoUploadTaskMapper;
    private final VideoMapper videoMapper;
    private final VideoUploadStorageService videoUploadStorageService;

    @Autowired
    public VideoUploadServiceImpl(VideoUploadTaskMapper videoUploadTaskMapper,
                                  VideoMapper videoMapper,
                                  VideoUploadStorageService videoUploadStorageService) {
        this.videoUploadTaskMapper = videoUploadTaskMapper;
        this.videoMapper = videoMapper;
        this.videoUploadStorageService = videoUploadStorageService;
    }

    @Override
    public VideoUploadInitVO initUpload(Long uid, VideoUploadInitDTO dto) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("init upload request is null");
        }

        String fileName = StringTool.normalizeRequired(dto.getFileName(), "fileName");
        String contentType = StringTool.normalizeOptional(dto.getContentType());
        long totalSize = requirePositive(dto.getTotalSize(), "totalSize");
        int chunkSize = requirePositive(dto.getChunkSize(), "chunkSize");
        int totalChunks = requirePositive(dto.getTotalChunks(), "totalChunks");

        if (totalSize > videoUploadStorageService.getMaxVideoSize()) {
            throw new IllegalArgumentException("video file is too large");
        }
        if (!videoUploadStorageService.isAllowedVideoType(contentType)) {
            throw new IllegalArgumentException("video content type is not allowed");
        }

        long expectedChunks = (totalSize + chunkSize - 1L) / chunkSize;
        if (expectedChunks != totalChunks) {
            throw new IllegalArgumentException("totalChunks does not match totalSize/chunkSize");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusHours(24);
        String tempRelativeDir = videoUploadStorageService.buildTempRelativeDir(uid, uploadId);

        VideoUploadTaskDO task = new VideoUploadTaskDO();
        task.setUploadId(uploadId);
        task.setUserId(uid);
        task.setFileName(fileName);
        task.setContentType(contentType);
        task.setFileSize(totalSize);
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setStatus(UploadTaskStatus.UPLOADING.code());
        task.setTempDir(tempRelativeDir);
        task.setExpireTime(expireTime);

        int rows = videoUploadTaskMapper.insert(task);
        if (rows != 1) {
            throw new RuntimeException("init upload task failed");
        }

        videoUploadStorageService.createDirectory(tempRelativeDir);

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

        videoUploadStorageService.saveChunk(task.getTempDir(), index, file, expectedChunkSize);
    }

    @Override
    public VideoUploadStatusVO getUploadStatus(Long uid, String uploadId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        VideoUploadTaskDO task = getOwnedTask(uid, uploadId);
        List<Integer> uploadedChunks = videoUploadStorageService.listUploadedChunkIndexes(task.getTempDir());

        VideoUploadStatusVO vo = new VideoUploadStatusVO();
        vo.setUploadId(task.getUploadId());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setUploadedChunks(uploadedChunks);
        vo.setUploadedChunkCount(uploadedChunks.size());
        vo.setCompleted(UploadTaskStatus.DONE.matches(task.getStatus()));
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

        List<Integer> uploadedChunks = videoUploadStorageService.listUploadedChunkIndexes(task.getTempDir());
        validateAllChunksPresent(task.getTotalChunks(), uploadedChunks);

        LambdaUpdateWrapper<VideoUploadTaskDO> markMerging = new LambdaUpdateWrapper<>();
        markMerging.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.UPLOADING.code())
                .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.MERGING.code());
        int updateRows = videoUploadTaskMapper.update(null, markMerging);
        if (updateRows != 1) {
            VideoUploadTaskDO latest = getOwnedTask(uid, uploadId);
            if (UploadTaskStatus.DONE.matches(latest.getStatus())) {
                return buildCompleteVO(latest);
            }
            throw new IllegalArgumentException("upload task is not in uploading status");
        }

        String finalRelativePath = null;
        try {
            finalRelativePath = videoUploadStorageService.buildFinalVideoRelativePath(task.getFileName());
            videoUploadStorageService.mergeChunks(task.getTempDir(), task.getTotalChunks(), finalRelativePath);

            String videoUrl = videoUploadStorageService.buildPublicUrl(finalRelativePath);
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
            int insertVideoRows = videoMapper.insert(video);
            if (insertVideoRows != 1 || video.getId() == null) {
                throw new RuntimeException("insert video failed");
            }

            LambdaUpdateWrapper<VideoUploadTaskDO> markDone = new LambdaUpdateWrapper<>();
            markDone.eq(VideoUploadTaskDO::getUploadId, task.getUploadId())
                    .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.MERGING.code())
                    .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.DONE.code())
                    .set(VideoUploadTaskDO::getFinalVideoId, video.getId())
                    .set(VideoUploadTaskDO::getFinalVideoUrl, videoUrl)
                    .set(VideoUploadTaskDO::getErrorMsg, null);
            int markDoneRows = videoUploadTaskMapper.update(null, markDone);
            if (markDoneRows != 1) {
                throw new RuntimeException("mark upload task done failed");
            }

            videoUploadStorageService.cleanupTempDir(task.getTempDir());

            VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
            vo.setUploadId(task.getUploadId());
            vo.setVideoId(video.getId());
            vo.setVideoUrl(videoUrl);
            return vo;
        } catch (Exception e) {
            markTaskFailed(task.getUploadId(), e.getMessage());
            if (finalRelativePath != null) {
                videoUploadStorageService.deleteByRelativePath(finalRelativePath);
            }
            throw new RuntimeException("complete upload failed", e);
        }
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
                && (task.getStatus() == null || UploadTaskStatus.UPLOADING.matches(task.getStatus()))) {
            LambdaUpdateWrapper<VideoUploadTaskDO> markExpired = new LambdaUpdateWrapper<>();
            markExpired.eq(VideoUploadTaskDO::getUploadId, normalizedUploadId)
                    .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.UPLOADING.code())
                    .set(VideoUploadTaskDO::getStatus, UploadTaskStatus.EXPIRED.code());
            videoUploadTaskMapper.update(null, markExpired);
            throw new IllegalArgumentException("upload task is expired");
        }
        return task;
    }

    private void ensureTaskUploadable(VideoUploadTaskDO task) {
        Integer status = task.getStatus();
        if (status == null || !UploadTaskStatus.UPLOADING.matches(status)) {
            throw new IllegalArgumentException("upload task is not uploadable");
        }
    }

    private long expectedChunkSize(VideoUploadTaskDO task, int index) {
        if (index < task.getTotalChunks() - 1) {
            return task.getChunkSize();
        }
        return task.getFileSize() - (long) task.getChunkSize() * (task.getTotalChunks() - 1);
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

    private void markTaskFailed(String uploadId, String message) {
        String errorMessage = StringTool.normalizeOptional(message);
        if (errorMessage != null && errorMessage.length() > 255) {
            errorMessage = errorMessage.substring(0, 255);
        }
        LambdaUpdateWrapper<VideoUploadTaskDO> markFailed = new LambdaUpdateWrapper<>();
        markFailed.eq(VideoUploadTaskDO::getUploadId, uploadId)
                .eq(VideoUploadTaskDO::getStatus, UploadTaskStatus.MERGING.code())
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
