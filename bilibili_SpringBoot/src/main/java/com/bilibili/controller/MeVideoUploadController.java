package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.dto.VideoUploadCompleteDTO;
import com.bilibili.model.dto.VideoUploadInitDTO;
import com.bilibili.model.vo.VideoUploadCompleteVO;
import com.bilibili.model.vo.VideoUploadInitVO;
import com.bilibili.model.vo.VideoUploadStatusVO;
import com.bilibili.service.VideoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/me/videos/uploads")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Video Upload", description = "Current user chunked video upload APIs")
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

    @PutMapping(value = "/{uploadId}/chunks/{index}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canAccessUploadTask(authentication, #uploadId)")
    @Operation(summary = "Upload video chunk")
    public Result<Void> uploadChunk(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable("uploadId") String uploadId,
                                    @PathVariable("index") Integer index,
                                    @RequestParam("file") MultipartFile file) {
        videoUploadService.uploadChunk(currentUser.getUid(), uploadId, index, file);
        return Result.success(null);
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
}