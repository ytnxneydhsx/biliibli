package com.bilibili.im.upload.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.upload.service.ImImageUploadService;
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
@Tag(name = "Me IM Image Upload", description = "Current user IM image upload APIs")
public class MeImImageUploadController {

    private final ImImageUploadService imImageUploadService;

    public MeImImageUploadController(ImImageUploadService imImageUploadService) {
        this.imImageUploadService = imImageUploadService;
    }

    @PostMapping(value = "/me/im/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
    @Operation(summary = "Upload my IM image")
    public Result<String> uploadMyImImage(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                          @RequestParam("file") MultipartFile file) {
        return Result.success(imImageUploadService.uploadImage(currentUser.getUid(), file));
    }
}
