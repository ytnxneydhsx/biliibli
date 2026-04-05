package com.bilibili.upload.group.service.impl;

import com.bilibili.storage.common.StoredFile;
import com.bilibili.storage.image.ImageStorageService;
import com.bilibili.storage.image.ImageStorageType;
import com.bilibili.upload.group.service.GroupAvatarUploadService;
import com.bilibili.im.group.service.ChatGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GroupAvatarUploadServiceImpl implements GroupAvatarUploadService {

    private final ImageStorageService imageStorageService;
    private final ChatGroupService chatGroupService;

    public GroupAvatarUploadServiceImpl(ImageStorageService imageStorageService,
                                        ChatGroupService chatGroupService) {
        this.imageStorageService = imageStorageService;
        this.chatGroupService = chatGroupService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadGroupAvatar(Long operatorUserId, Long groupId, MultipartFile file) {
        if (operatorUserId == null || operatorUserId <= 0) {
            throw new IllegalArgumentException("operatorUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        StoredFile storedFile = imageStorageService.saveImage(file, ImageStorageType.GROUP_AVATAR);
        String avatarUrl = storedFile.getPublicUrl();
        try {
            String previousAvatarUrl = chatGroupService.updateGroupAvatar(groupId, operatorUserId, avatarUrl);
            imageStorageService.deleteByPublicUrl(previousAvatarUrl);
            return avatarUrl;
        } catch (RuntimeException e) {
            imageStorageService.deleteByPublicUrl(avatarUrl);
            throw e;
        }
    }
}
