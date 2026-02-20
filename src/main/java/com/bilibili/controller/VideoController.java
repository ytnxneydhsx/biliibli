package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public Result<List<VideoVO>> listVideos(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(videoService.listHomepageVideos(null, pageNo, pageSize));
    }

    @GetMapping("/search")
    public Result<List<VideoVO>> searchVideos(@RequestParam("keyword") String keyword,
                                              @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                              @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(videoService.searchVideos(keyword, pageNo, pageSize));
    }

    @GetMapping("/{videoId}")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable("videoId") Long videoId,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Long currentUid = currentUser == null ? null : currentUser.getUid();
        return Result.success(videoService.getVideoDetail(videoId, currentUid));
    }

    @PostMapping("/{videoId}/views")
    public Result<Void> increaseViewCount(@PathVariable("videoId") Long videoId) {
        videoService.increaseViewCount(videoId);
        return Result.success(null);
    }
}
