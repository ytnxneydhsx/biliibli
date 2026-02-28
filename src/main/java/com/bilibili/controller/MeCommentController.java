package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.model.dto.CommentCreateDTO;
import com.bilibili.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeCommentController {

    private final CommentService commentService;

    @Autowired
    public MeCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/videos/{videoId}/comments")
    public Result<Long> createComment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable("videoId") Long videoId,
                                      @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.createComment(currentUser.getUid(), videoId, dto));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("@authz.canDeleteComment(authentication, #commentId)")
    public Result<Void> deleteComment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable("commentId") Long commentId) {
        commentService.deleteComment(currentUser.getUid(), commentId);
        return Result.success(null);
    }

    @PostMapping("/comments/{commentId}/likes")
    public Result<Void> likeComment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable("commentId") Long commentId) {
        commentService.likeComment(currentUser.getUid(), commentId);
        return Result.success(null);
    }

    @DeleteMapping("/comments/{commentId}/likes")
    public Result<Void> unlikeComment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable("commentId") Long commentId) {
        commentService.unlikeComment(currentUser.getUid(), commentId);
        return Result.success(null);
    }
}
