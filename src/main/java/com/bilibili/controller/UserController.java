package com.bilibili.controller;

import com.bilibili.common.result.Result;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.entity.UserDO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<UserDO> login(@RequestBody UserLoginDTO dto) {
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

}
