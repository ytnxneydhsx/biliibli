package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.vo.PageVO;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoAppService videoAppService;

    @Autowired
    public VideoController(VideoAppService videoAppService) {
        this.videoAppService = videoAppService;
    }

    @GetMapping
    public Result<PageVO<VideoVO>> listVideos(PageQueryDTO pageQuery) {
        return Result.success(PageVO.from(videoAppService.listVideos(pageQuery.getPageNo(), pageQuery.getPageSize())));
    }

    @GetMapping("/rank")
    public Result<PageVO<VideoRankVO>> listVideoRank(PageQueryDTO pageQuery) {
        return Result.success(PageVO.from(videoAppService.listVideoRank(pageQuery.getPageNo(), pageQuery.getPageSize())));
    }

    @GetMapping("/{videoId}")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable("videoId") Long videoId,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Long currentUid = currentUser == null ? null : currentUser.getUid();
        return Result.success(videoAppService.getVideoDetail(videoId, currentUid));
    }

    @PostMapping("/{videoId}/views")
    public Result<Void> increaseViewCount(@PathVariable("videoId") Long videoId) {
        videoAppService.increaseViewCount(videoId);
        return Result.success(null);
    }
}
