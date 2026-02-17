package com.bilibili.service;

import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.entity.UserDO;
import com.bilibili.model.vo.UserProfileVO;

public interface UserService {

    Long register(UserRegisterDTO dto);

    UserDO login(UserLoginDTO dto);

    UserProfileVO getPublicProfile(Long uid);

    void updatePublicProfile(Long uid, UserProfileUpdateDTO dto);

}
