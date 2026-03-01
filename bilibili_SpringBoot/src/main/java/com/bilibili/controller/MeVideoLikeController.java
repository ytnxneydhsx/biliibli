package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/videos")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Video Like", description = "Current user video like APIs")
public class MeVideoLikeController {

    private final VideoService videoService;

    @Autowired
    public MeVideoLikeController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/{videoId}/likes")
    @Operation(summary = "Like video")
    public Result<Void> likeVideo(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                  @PathVariable("videoId") Long videoId) {
        videoService.likeVideo(currentUser.getUid(), videoId);
        return Result.success(null);
    }

    @DeleteMapping("/{videoId}/likes")
    @Operation(summary = "Unlike video")
    public Result<Void> unlikeVideo(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable("videoId") Long videoId) {
        videoService.unlikeVideo(currentUser.getUid(), videoId);
        return Result.success(null);
    }
}