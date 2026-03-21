package com.bilibili.upload.video.service;

import com.bilibili.upload.video.model.dto.VideoUploadCompleteDTO;
import com.bilibili.upload.video.model.dto.VideoUploadInitDTO;
import com.bilibili.upload.video.model.dto.VideoUploadPartSignDTO;
import com.bilibili.upload.video.model.vo.VideoUploadCompleteVO;
import com.bilibili.upload.video.model.vo.VideoUploadInitVO;
import com.bilibili.upload.video.model.vo.VideoUploadPartSignVO;
import com.bilibili.upload.video.model.vo.VideoUploadStatusVO;

public interface VideoUploadService {

    VideoUploadInitVO initUpload(Long uid, VideoUploadInitDTO dto);

    VideoUploadPartSignVO signUploadParts(Long uid, String uploadId, VideoUploadPartSignDTO dto);

    VideoUploadStatusVO getUploadStatus(Long uid, String uploadId);

    VideoUploadCompleteVO completeUpload(Long uid, String uploadId, VideoUploadCompleteDTO dto);

    void cancelUpload(Long uid, String uploadId);
}
