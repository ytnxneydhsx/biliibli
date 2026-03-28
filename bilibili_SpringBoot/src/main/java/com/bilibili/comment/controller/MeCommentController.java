package com.bilibili.comment.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.comment.model.dto.CommentCreateDTO;
import com.bilibili.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Comment", description = "Current user comment APIs")
public class MeCommentController {

    private final CommentService commentService;

    @Autowired
    public MeCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/videos/{videoId}/comments")
    @PreAuthorize("@accessAuthz.canComment(authentication)")
    @Operation(summary = "Create comment")
    public Result<Long> createComment(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable("videoId") Long videoId,
                                      @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.createComment(currentUser.getUid(), videoId, dto));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("@authz.canDeleteComment(authentication, #commentId)")
    @Operation(summary = "Delete comment")
    public Result<Void> deleteComment(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable("commentId") Long commentId) {
        commentService.deleteComment(currentUser.getUid(), commentId);
        return Result.success(null);
    }

    @PostMapping("/comments/{commentId}/likes")
    @PreAuthorize("@accessAuthz.canLike(authentication)")
    @Operation(summary = "Like comment")
    public Result<Void> likeComment(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable("commentId") Long commentId) {
        commentService.likeComment(currentUser.getUid(), commentId);
        return Result.success(null);
    }

    @DeleteMapping("/comments/{commentId}/likes")
    @PreAuthorize("@accessAuthz.canLike(authentication)")
    @Operation(summary = "Unlike comment")
    public Result<Void> unlikeComment(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable("commentId") Long commentId) {
        commentService.unlikeComment(currentUser.getUid(), commentId);
        return Result.success(null);
    }
}
