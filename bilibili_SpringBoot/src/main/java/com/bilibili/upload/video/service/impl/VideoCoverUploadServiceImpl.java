package com.bilibili.upload.video.service.impl;

import com.bilibili.storage.common.StoredFile;
import com.bilibili.storage.image.ImageStorageService;
import com.bilibili.storage.image.ImageStorageType;
import com.bilibili.upload.video.service.VideoCoverUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoCoverUploadServiceImpl implements VideoCoverUploadService {

    private final ImageStorageService imageStorageService;

    public VideoCoverUploadServiceImpl(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @Override
    public String uploadCover(Long uid, MultipartFile file) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        StoredFile storedFile = imageStorageService.saveImage(file, ImageStorageType.VIDEO_COVER);
        return storedFile.getPublicUrl();
    }
}
