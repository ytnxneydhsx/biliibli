package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/videos")
public class MeVideoLikeController {

    private final VideoService videoService;

    @Autowired
    public MeVideoLikeController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/{videoId}/likes")
    public Result<Void> likeVideo(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                  @PathVariable("videoId") Long videoId) {
        videoService.likeVideo(currentUser.getUid(), videoId);
        return Result.success(null);
    }

    @DeleteMapping("/{videoId}/likes")
    public Result<Void> unlikeVideo(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable("videoId") Long videoId) {
        videoService.unlikeVideo(currentUser.getUid(), videoId);
        return Result.success(null);
    }
}
