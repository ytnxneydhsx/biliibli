package com.bilibili.search.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.search.model.vo.UserSearchVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.search.service.SearchService;
import com.bilibili.tool.StringTool;
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
                                              PageQueryDTO pageQuery,
                                              @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser != null && currentUser.getUid() != null && !StringTool.isBlank(keyword)) {
            searchService.recordVideoSearchHistory(currentUser.getUid(), keyword);
        }
        return Result.success(searchService.searchVideos(keyword, categoryId, pageQuery));
    }

    @GetMapping("/users")
    @Operation(summary = "Search users by nickname (paged)")
    public Result<PageVO<UserSearchVO>> searchUsers(@RequestParam(value = "nickname") String nickname,
                                                    @RequestParam(value = "timeOrder", required = false, defaultValue = "asc") String timeOrder,
                                                    PageQueryDTO pageQuery) {
        return Result.success(PageVO.from(searchService.searchUsers(nickname, timeOrder, pageQuery)));
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
