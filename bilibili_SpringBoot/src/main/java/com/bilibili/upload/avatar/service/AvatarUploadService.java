package com.bilibili.upload.avatar.service;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarUploadService {

    String uploadAvatar(Long uid, MultipartFile file);
}
