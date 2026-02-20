package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.dto.VideoUploadCompleteDTO;
import com.bilibili.model.dto.VideoUploadInitDTO;
import com.bilibili.model.vo.VideoUploadCompleteVO;
import com.bilibili.model.vo.VideoUploadInitVO;
import com.bilibili.model.vo.VideoUploadStatusVO;
import com.bilibili.service.VideoUploadService;
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
public class MeVideoUploadController {

    private final VideoUploadService videoUploadService;

    @Autowired
    public MeVideoUploadController(VideoUploadService videoUploadService) {
        this.videoUploadService = videoUploadService;
    }

    @PostMapping("/init-session")
    @PreAuthorize("isAuthenticated()")
    public Result<VideoUploadInitVO> initUpload(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @RequestBody VideoUploadInitDTO dto) {
        return Result.success(videoUploadService.initUpload(currentUser.getUid(), dto));
    }

    @PutMapping(value = "/{uploadId}/chunks/{index}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Result<Void> uploadChunk(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable("uploadId") String uploadId,
                                    @PathVariable("index") Integer index,
                                    @RequestParam("file") MultipartFile file) {
        videoUploadService.uploadChunk(currentUser.getUid(), uploadId, index, file);
        return Result.success(null);
    }

    @GetMapping("/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    public Result<VideoUploadStatusVO> getUploadStatus(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable("uploadId") String uploadId) {
        return Result.success(videoUploadService.getUploadStatus(currentUser.getUid(), uploadId));
    }

    @PostMapping("/{uploadId}/complete")
    @PreAuthorize("isAuthenticated()")
    public Result<VideoUploadCompleteVO> completeUpload(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable("uploadId") String uploadId,
                                                        @RequestBody VideoUploadCompleteDTO dto) {
        return Result.success(videoUploadService.completeUpload(currentUser.getUid(), uploadId, dto));
    }
}
