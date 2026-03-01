package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/videos")
@Tag(name = "Video", description = "Video browse and stats APIs")
public class VideoController {

    private final VideoAppService videoAppService;

    @Autowired
    public VideoController(VideoAppService videoAppService) {
        this.videoAppService = videoAppService;
    }

    @GetMapping
    @Operation(summary = "List videos (paged)")
    public Result<List<VideoVO>> listVideos(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(videoAppService.listVideos(pageNo, pageSize));
    }

    @GetMapping("/rank")
    @Operation(summary = "List video ranking (paged)")
    public Result<List<VideoRankVO>> listVideoRank(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(videoAppService.listVideoRank(pageNo, pageSize));
    }

    @GetMapping("/{videoId}")
    @Operation(summary = "Get video detail")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable("videoId") Long videoId,
                                                @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Long currentUid = currentUser == null ? null : currentUser.getUid();
        return Result.success(videoAppService.getVideoDetail(videoId, currentUid));
    }

    @PostMapping("/{videoId}/views")
    @Operation(summary = "Increase video view count")
    public Result<Void> increaseViewCount(@PathVariable("videoId") Long videoId) {
        videoAppService.increaseViewCount(videoId);
        return Result.success(null);
    }
}