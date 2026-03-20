package com.bilibili.video.controller;

import com.bilibili.common.result.Result;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "User Video", description = "User video list APIs")
public class UserVideoController {

    private final VideoService videoService;

    @Autowired
    public UserVideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/{uid}/videos")
    @Operation(summary = "List published videos by user (paged)")
    public Result<PageVO<VideoVO>> listPublishedVideos(@PathVariable("uid") Long uid,
                                                       @RequestParam(value = "title", required = false) String title,
                                                       PageQueryDTO pageQuery) {
        return Result.success(PageVO.from(videoService.listPublishedVideos(uid, title, pageQuery)));
    }
}
