package com.bilibili.controller;

import com.bilibili.common.result.Result;
import com.bilibili.model.dto.UserLoginDTO;
import com.bilibili.model.dto.UserRegisterDTO;
import com.bilibili.model.vo.UserLoginVO;
import com.bilibili.model.vo.UserProfileVO;
import com.bilibili.security.JwtTokenService;
import com.bilibili.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public UserController(UserService userService, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO dto) {
        UserLoginVO loginVO = userService.login(dto);
        String token = jwtTokenService.generateToken(loginVO.getUid());
        loginVO.setToken(token);
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT stateless mode: client deletes local token.
        return Result.success(null);
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @GetMapping("/{uid}")
    public Result<UserProfileVO> getPublicProfile(@PathVariable("uid") Long uid) {
        return Result.success(userService.getPublicProfile(uid));
    }

}
