package com.bilibili.im.upload.service.impl;

import com.bilibili.access.service.UserAccessService;
import com.bilibili.storage.common.StoredFile;
import com.bilibili.storage.image.ImageStorageService;
import com.bilibili.storage.image.ImageStorageType;
import com.bilibili.im.upload.service.ImImageUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImImageUploadServiceImpl implements ImImageUploadService {

    private final UserAccessService userAccessService;
    private final ImageStorageService imageStorageService;

    public ImImageUploadServiceImpl(UserAccessService userAccessService,
                                    ImageStorageService imageStorageService) {
        this.userAccessService = userAccessService;
        this.imageStorageService = imageStorageService;
    }

    @Override
    public String uploadImage(Long uid, MultipartFile file) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        userAccessService.validateCanSendImMessage(uid);
        StoredFile storedFile = imageStorageService.saveImage(file, ImageStorageType.IM_MESSAGE);
        return storedFile.getPublicUrl();
    }
}
