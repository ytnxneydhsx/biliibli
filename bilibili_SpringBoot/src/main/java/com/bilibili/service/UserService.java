package com.bilibili.service;

import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    Long register(UserRegisterDTO dto);

    UserLoginVO login(UserLoginDTO dto);

    UserProfileVO getPublicProfile(Long uid);

    void updatePublicProfile(Long uid, UserProfileUpdateDTO dto);

    String uploadAvatar(Long uid, MultipartFile file);

}
