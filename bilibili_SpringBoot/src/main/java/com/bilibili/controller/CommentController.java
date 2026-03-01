package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.vo.CommentVO;
import com.bilibili.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/videos")
@Tag(name = "Comment", description = "Comment query APIs")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{videoId}/comments")
    @Operation(summary = "List video comments (paged)")
    public Result<List<CommentVO>> listComments(@PathVariable("videoId") Long videoId,
                                                @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Long currentUid = currentUser == null ? null : currentUser.getUid();
        return Result.success(commentService.listComments(videoId, pageNo, pageSize, currentUid));
    }
}