package com.bilibili.controller;

import com.bilibili.common.result.Result;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @GetMapping("/{uid}")
    public Result<UserProfileVO> getPublicProfile(@PathVariable("uid") Long uid) {
        return Result.success(userService.getPublicProfile(uid));
    }

    @PutMapping("/{uid}/profile")
    public Result<Void> updatePublicProfile(@PathVariable("uid") Long uid,
                                            @RequestBody UserProfileUpdateDTO dto) {
        userService.updatePublicProfile(uid, dto);
        return Result.success(null);
    }

    @PostMapping(value = "/{uid}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@PathVariable("uid") Long uid,
                                       @RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(uid, file));
    }

}
