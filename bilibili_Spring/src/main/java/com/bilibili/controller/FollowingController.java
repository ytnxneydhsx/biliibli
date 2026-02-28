package com.bilibili.controller;

import com.bilibili.common.result.Result;
import com.bilibili.model.vo.FollowersQueryVO;
import com.bilibili.service.FollowingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class FollowingController {

    private final FollowingService followingService;

    @Autowired
    public FollowingController(FollowingService followingService) {
        this.followingService = followingService;
    }

    @GetMapping("/{uid}/followers")
    public Result<List<FollowersQueryVO>> followers(@PathVariable("uid") Long uid) {
        return Result.success(followingService.followersQuery(uid));
    }

    @GetMapping("/{uid}/followings")
    public Result<List<FollowersQueryVO>> followings(@PathVariable("uid") Long uid) {
        return Result.success(followingService.followingsQuery(uid));
    }

    @GetMapping("/{uid}/friends")
    public Result<List<FollowersQueryVO>> friends(@PathVariable("uid") Long uid) {
        return Result.success(followingService.friendsQuery(uid));
    }
}
