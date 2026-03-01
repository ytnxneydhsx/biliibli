package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.service.FollowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me Following", description = "Current user following management APIs")
public class MeFollowingController {

    private final FollowingService followingService;

    @Autowired
    public MeFollowingController(FollowingService followingService) {
        this.followingService = followingService;
    }

    @PostMapping("/followings/{targetUid}")
    @Operation(summary = "Follow user")
    public Result<Void> follow(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                               @PathVariable("targetUid") Long targetUid) {
        followingService.follow(currentUser.getUid(), targetUid);
        return Result.success(null);
    }

    @DeleteMapping("/followings/{targetUid}")
    @Operation(summary = "Unfollow user")
    public Result<Void> unfollow(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
                                 @PathVariable("targetUid") Long targetUid) {
        followingService.unfollow(currentUser.getUid(), targetUid);
        return Result.success(null);
    }
}