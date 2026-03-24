package com.bilibili.video.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.application.VideoApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
@Tag(name = "Video", description = "Video browse and stats APIs")
public class VideoController {

    private final VideoApplicationService videoApplicationService;

    @Autowired
    public VideoController(VideoApplicationService videoApplicationService) {
        this.videoApplicationService = videoApplicationService;
    }

    @GetMapping
    @Operation(summary = "List videos (paged)")
    public Result<PageVO<VideoVO>> listVideos(PageQueryDTO pageQuery) {
        return Result.success(PageVO.from(videoApplicationService.listVideos(pageQuery)));
    }

    @GetMapping("/rank")
    @Operation(summary = "List video ranking (paged)")
    public Result<PageVO<VideoRankVO>> listVideoRank(PageQueryDTO pageQuery) {
        return Result.success(PageVO.from(videoApplicationService.listVideoRank(pageQuery)));
    }

    @GetMapping("/{videoId}")
    @Operation(summary = "Get video detail")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable("videoId") Long videoId,
                                                @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Long currentUid = currentUser == null ? null : currentUser.getUid();
        return Result.success(videoApplicationService.getVideoDetail(videoId, currentUid));
    }

    @PostMapping("/{videoId}/views")
    @Operation(summary = "Increase video view count")
    public Result<Void> increaseViewCount(@PathVariable("videoId") Long videoId) {
        videoApplicationService.increaseViewCount(videoId);
        return Result.success(null);
    }
}
