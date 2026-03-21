package com.bilibili.upload.avatar.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.upload.avatar.service.AvatarUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Avatar Upload", description = "Current user avatar upload APIs")
public class MeAvatarUploadController {

    private final AvatarUploadService avatarUploadService;

    public MeAvatarUploadController(AvatarUploadService avatarUploadService) {
        this.avatarUploadService = avatarUploadService;
    }

    @PostMapping(value = "/me/uploads/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload my avatar")
    public Result<String> uploadMyAvatar(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @RequestParam("file") MultipartFile file) {
        return Result.success(avatarUploadService.uploadAvatar(currentUser.getUid(), file));
    }
}
