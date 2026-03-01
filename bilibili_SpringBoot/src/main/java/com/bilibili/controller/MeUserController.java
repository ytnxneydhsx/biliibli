package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.dto.UserProfileUpdateDTO;
import com.bilibili.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me User", description = "Current user profile APIs")
public class MeUserController {

    private final UserService userService;

    @Autowired
    public MeUserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my profile")
    public Result<Void> updateMyProfile(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @RequestBody UserProfileUpdateDTO dto) {
        userService.updatePublicProfile(currentUser.getUid(), dto);
        return Result.success(null);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload my avatar")
    public Result<String> uploadMyAvatar(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(currentUser.getUid(), file));
    }
}