package com.bilibili.upload.video.service;

import org.springframework.web.multipart.MultipartFile;

public interface VideoCoverUploadService {

    String uploadCover(Long uid, MultipartFile file);
}
