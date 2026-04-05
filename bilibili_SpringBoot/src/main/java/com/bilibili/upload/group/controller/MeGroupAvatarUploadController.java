package com.bilibili.upload.group.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.upload.group.service.GroupAvatarUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/me/im/groups")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Group Avatar Upload", description = "Current user group avatar upload APIs")
public class MeGroupAvatarUploadController {

    private final GroupAvatarUploadService groupAvatarUploadService;

    public MeGroupAvatarUploadController(GroupAvatarUploadService groupAvatarUploadService) {
        this.groupAvatarUploadService = groupAvatarUploadService;
    }

    @PutMapping(value = "/{groupId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload current group avatar")
    public Result<String> uploadGroupAvatar(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @RequestPart("file") MultipartFile file) {
        return Result.success(groupAvatarUploadService.uploadGroupAvatar(currentUser.getUid(), groupId, file));
    }
}
