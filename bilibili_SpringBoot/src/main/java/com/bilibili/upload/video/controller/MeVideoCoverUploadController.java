package com.bilibili.upload.video.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.upload.video.service.VideoCoverUploadService;
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
@Tag(name = "Me Video Cover Upload", description = "Current user video cover upload APIs")
public class MeVideoCoverUploadController {

    private final VideoCoverUploadService videoCoverUploadService;

    public MeVideoCoverUploadController(VideoCoverUploadService videoCoverUploadService) {
        this.videoCoverUploadService = videoCoverUploadService;
    }

    @PostMapping(value = "/me/uploads/video-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload my video cover")
    public Result<String> uploadMyVideoCover(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @RequestParam("file") MultipartFile file) {
        return Result.success(videoCoverUploadService.uploadCover(currentUser.getUid(), file));
    }
}
