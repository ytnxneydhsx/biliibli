package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilibili.common.enums.UserStatus;
import com.bilibili.mapper.UserInfoMapper;
import com.bilibili.mapper.UserMapper;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.entity.UserDO;
import com.bilibili.model.entity.UserInfoDO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.UserService;
import com.bilibili.storage.StorageService;
import com.bilibili.storage.StoredFile;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final StorageService storageService;

    @Autowired
    public UserServiceImpl(UserMapper userMapper,
                           UserInfoMapper userInfoMapper,
                           StorageService storageService) {
        this.userMapper = userMapper;
        this.userInfoMapper = userInfoMapper;
        this.storageService = storageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("register request is null");
        }
        if (StringTool.isBlank(dto.getUsername()) || StringTool.isBlank(dto.getNickname())
                || StringTool.isBlank(dto.getPassword()) || StringTool.isBlank(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("username/nickname/password cannot be blank");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("password and confirmPassword are not equal");
        }

        String username = StringTool.normalizeRequired(dto.getUsername(), "username");
        String nickname = StringTool.normalizeRequired(dto.getNickname(), "nickname");

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username);
        UserDO existed = userMapper.selectOne(queryWrapper);
        if (existed != null) {
            throw new IllegalArgumentException("username already exists");
        }

        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(encryptPassword(dto.getPassword()));
        user.setStatus(UserStatus.NORMAL.code());
        int userRows = userMapper.insert(user);
        if (userRows != 1 || user.getId() == null) {
            throw new RuntimeException("insert t_user failed");
        }

        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(user.getId());
        userInfo.setNickname(nickname);
        userInfo.setFollowingCount(0);
        userInfo.setFollowerCount(0);
        int userInfoRows = userInfoMapper.insert(userInfo);
        if (userInfoRows != 1) {
            throw new RuntimeException("insert t_user_info failed");
        }
        return user.getId();
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("login request is null");
        }
        if (StringTool.isBlank(dto.getUsername()) || StringTool.isBlank(dto.getPassword())) {
            throw new IllegalArgumentException("username/password cannot be blank");
        }
        String username = StringTool.normalizeRequired(dto.getUsername(), "username");
        String passwordHash = encryptPassword(dto.getPassword());

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username)
                .eq(UserDO::getPassword, passwordHash);
        UserDO user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new IllegalArgumentException("username or password is incorrect");
        }

        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setUid(user.getId());
        loginVO.setUsername(user.getUsername());
        return loginVO;
    }

    @Override
    public UserProfileVO getPublicProfile(Long uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        UserInfoDO userInfo = getUserInfoByUid(uid);

        UserProfileVO profile = new UserProfileVO();
        profile.setUid(userInfo.getUserId());
        profile.setNickname(userInfo.getNickname());
        profile.setAvatar(userInfo.getAvatarUrl());
        profile.setSign(userInfo.getSign());
        profile.setFollowerCount(userInfo.getFollowerCount());
        profile.setFollowingCount(userInfo.getFollowingCount());
        return profile;
    }

    @Override
    public void updatePublicProfile(Long uid, UserProfileUpdateDTO dto) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("update request is null");
        }

        String nickname = StringTool.normalizeOptional(dto.getNickname());
        String sign = StringTool.normalizeOptional(dto.getSign());
        if (nickname == null && sign == null) {
            throw new IllegalArgumentException("nothing to update");
        }

        LambdaUpdateWrapper<UserInfoDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserInfoDO::getUserId, uid);
        if (nickname != null) {
            uw.set(UserInfoDO::getNickname, nickname);
        }
        if (sign != null) {
            uw.set(UserInfoDO::getSign, sign);
        }

        int rows = userInfoMapper.update(null, uw);
        if (rows != 1) {
            throw new IllegalArgumentException("user not found or no changes");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long uid, MultipartFile file) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        UserInfoDO userInfo = getUserInfoByUid(uid);
        StoredFile storedFile = storageService.saveAvatar(file);
        String avatarUrl = storedFile.getPublicUrl();

        LambdaUpdateWrapper<UserInfoDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserInfoDO::getUserId, uid)
                .set(UserInfoDO::getAvatarUrl, avatarUrl);
        int rows = userInfoMapper.update(null, uw);
        if (rows != 1) {
            storageService.deleteByPublicUrl(avatarUrl);
            throw new IllegalArgumentException("user not found");
        }

        storageService.deleteByPublicUrl(userInfo.getAvatarUrl());
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

    private static String encryptPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String item = Integer.toHexString(b & 0xff);
                if (item.length() == 1) {
                    hex.append('0');
                }
                hex.append(item);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
