package com.bilibili.upload.service;

import com.bilibili.upload.model.dto.VideoUploadCompleteDTO;
import com.bilibili.upload.model.dto.VideoUploadInitDTO;
import com.bilibili.upload.model.vo.VideoUploadCompleteVO;
import com.bilibili.upload.model.vo.VideoUploadInitVO;
import com.bilibili.upload.model.vo.VideoUploadStatusVO;
import org.springframework.web.multipart.MultipartFile;

public interface VideoUploadService {

    VideoUploadInitVO initUpload(Long uid, VideoUploadInitDTO dto);

    void uploadChunk(Long uid, String uploadId, Integer index, MultipartFile file);

    VideoUploadStatusVO getUploadStatus(Long uid, String uploadId);

    VideoUploadCompleteVO completeUpload(Long uid, String uploadId, VideoUploadCompleteDTO dto);
}
