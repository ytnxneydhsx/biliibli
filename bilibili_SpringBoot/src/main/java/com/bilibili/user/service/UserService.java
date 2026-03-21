package com.bilibili.user.service;

import com.bilibili.user.model.dto.UserLoginDTO;
import com.bilibili.user.model.dto.UserProfileUpdateDTO;
import com.bilibili.user.model.dto.UserRegisterDTO;
import com.bilibili.user.model.vo.UserLoginVO;
import com.bilibili.user.model.vo.UserProfileVO;

public interface UserService {

    Long register(UserRegisterDTO dto);

    UserLoginVO login(UserLoginDTO dto);

    UserProfileVO getPublicProfile(Long uid);

    void updatePublicProfile(Long uid, UserProfileUpdateDTO dto);
}
