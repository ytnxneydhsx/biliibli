package com.bilibili.upload.group.service;

import org.springframework.web.multipart.MultipartFile;

public interface GroupAvatarUploadService {

    String uploadGroupAvatar(Long operatorUserId, Long groupId, MultipartFile file);
}
