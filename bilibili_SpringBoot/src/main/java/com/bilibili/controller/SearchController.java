package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Search and search history APIs")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/videos")
    @Operation(summary = "Search videos")
    public Result<List<VideoVO>> searchVideos(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                                              @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                              @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                              @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser != null && currentUser.getUid() != null && keyword != null && !keyword.trim().isEmpty()) {
            searchService.recordVideoSearchHistory(currentUser.getUid(), keyword);
        }
        return Result.success(searchService.searchVideos(keyword, categoryId, pageNo, pageSize));
    }

    @GetMapping("/videos/history")
    @Operation(summary = "List my video search history")
    public Result<List<String>> listMyVideoSearchHistory(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.getUid() == null) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(searchService.listVideoSearchHistory(currentUser.getUid()));
    }
}