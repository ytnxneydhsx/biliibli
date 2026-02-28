package com.bilibili.service;

import com.bilibili.model.vo.FollowersQueryVO;

import java.util.List;

public interface FollowingService {

    List<FollowersQueryVO> followersQuery(Long uid);

    List<FollowersQueryVO> followingsQuery(Long uid);

    List<FollowersQueryVO> friendsQuery(Long uid);

    void follow(Long uid, Long targetUid);

    void unfollow(Long uid, Long targetUid);
}
