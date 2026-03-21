package com.bilibili.upload.avatar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilibili.storage.common.StoredFile;
import com.bilibili.storage.image.ImageStorageService;
import com.bilibili.storage.image.ImageStorageType;
import com.bilibili.upload.avatar.service.AvatarUploadService;
import com.bilibili.user.mapper.UserInfoMapper;
import com.bilibili.user.model.entity.UserInfoDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarUploadServiceImpl implements AvatarUploadService {

    private final UserInfoMapper userInfoMapper;
    private final ImageStorageService imageStorageService;

    public AvatarUploadServiceImpl(UserInfoMapper userInfoMapper, ImageStorageService imageStorageService) {
        this.userInfoMapper = userInfoMapper;
        this.imageStorageService = imageStorageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long uid, MultipartFile file) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        UserInfoDO userInfo = getUserInfoByUid(uid);
        StoredFile storedFile = imageStorageService.saveImage(file, ImageStorageType.AVATAR);
        String avatarUrl = storedFile.getPublicUrl();

        LambdaUpdateWrapper<UserInfoDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserInfoDO::getUserId, uid)
                .set(UserInfoDO::getAvatarUrl, avatarUrl);
        int rows = userInfoMapper.update(null, uw);
        if (rows != 1) {
            imageStorageService.deleteByPublicUrl(avatarUrl);
            throw new IllegalArgumentException("user not found");
        }

        imageStorageService.deleteByPublicUrl(userInfo.getAvatarUrl());
        return avatarUrl;
    }

    private UserInfoDO getUserInfoByUid(Long uid) {
        LambdaQueryWrapper<UserInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfoDO::getUserId, uid);
        UserInfoDO userInfo = userInfoMapper.selectOne(queryWrapper);
        if (userInfo == null) {
            throw new IllegalArgumentException("user not found");
        }
        return userInfo;
    }
}
