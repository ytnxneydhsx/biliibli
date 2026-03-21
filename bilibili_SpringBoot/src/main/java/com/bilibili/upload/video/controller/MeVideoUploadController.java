package com.bilibili.upload.video.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.upload.video.model.dto.VideoUploadCompleteDTO;
import com.bilibili.upload.video.model.dto.VideoUploadInitDTO;
import com.bilibili.upload.video.model.dto.VideoUploadPartSignDTO;
import com.bilibili.upload.video.model.vo.VideoUploadCompleteVO;
import com.bilibili.upload.video.model.vo.VideoUploadInitVO;
import com.bilibili.upload.video.model.vo.VideoUploadPartSignVO;
import com.bilibili.upload.video.model.vo.VideoUploadStatusVO;
import com.bilibili.upload.video.service.VideoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/videos/uploads")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Video Upload", description = "Current user multipart video upload APIs")
public class MeVideoUploadController {

    private final VideoUploadService videoUploadService;

    @Autowired
    public MeVideoUploadController(VideoUploadService videoUploadService) {
        this.videoUploadService = videoUploadService;
    }

    @PostMapping("/init-session")
    @Operation(summary = "Initialize upload session")
    public Result<VideoUploadInitVO> initUpload(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @RequestBody VideoUploadInitDTO dto) {
        return Result.success(videoUploadService.initUpload(currentUser.getUid(), dto));
    }

    @PostMapping("/{uploadId}/parts/sign")
    @PreAuthorize("@authz.canAccessUploadTask(authentication, #uploadId)")
    @Operation(summary = "Sign multipart upload part URLs")
    public Result<VideoUploadPartSignVO> signUploadParts(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                                         @PathVariable("uploadId") String uploadId,
                                                         @RequestBody VideoUploadPartSignDTO dto) {
        return Result.success(videoUploadService.signUploadParts(currentUser.getUid(), uploadId, dto));
    }

    @GetMapping("/{uploadId}")
    @PreAuthorize("@authz.canAccessUploadTask(authentication, #uploadId)")
    @Operation(summary = "Get upload status")
    public Result<VideoUploadStatusVO> getUploadStatus(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable("uploadId") String uploadId) {
        return Result.success(videoUploadService.getUploadStatus(currentUser.getUid(), uploadId));
    }

    @PostMapping("/{uploadId}/complete")
    @PreAuthorize("@authz.canAccessUploadTask(authentication, #uploadId)")
    @Operation(summary = "Complete upload")
    public Result<VideoUploadCompleteVO> completeUpload(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable("uploadId") String uploadId,
                                                        @RequestBody VideoUploadCompleteDTO dto) {
        return Result.success(videoUploadService.completeUpload(currentUser.getUid(), uploadId, dto));
    }

    @DeleteMapping("/{uploadId}")
    @PreAuthorize("@authz.canAccessUploadTask(authentication, #uploadId)")
    @Operation(summary = "Cancel upload")
    public Result<Void> cancelUpload(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                     @PathVariable("uploadId") String uploadId) {
        videoUploadService.cancelUpload(currentUser.getUid(), uploadId);
        return Result.success(null);
    }
}
