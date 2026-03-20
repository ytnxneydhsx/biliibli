package com.bilibili.following.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.enums.UserStatus;
import com.bilibili.following.mapper.FollowingMapper;
import com.bilibili.user.mapper.UserInfoMapper;
import com.bilibili.user.mapper.UserMapper;
import com.bilibili.following.model.entity.FollowingDO;
import com.bilibili.user.model.entity.UserDO;
import com.bilibili.user.model.entity.UserInfoDO;
import com.bilibili.following.model.vo.FollowersQueryVO;
import com.bilibili.following.service.FollowingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowersService implements FollowingService {

    private final FollowingMapper followingMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserMapper userMapper;

    @Autowired
    public FollowersService(FollowingMapper followingMapper,
                            UserInfoMapper userInfoMapper,
                            UserMapper userMapper) {
        this.followingMapper = followingMapper;
        this.userInfoMapper = userInfoMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<FollowersQueryVO> followersQuery(Long uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        LambdaQueryWrapper<FollowingDO> followingQuery = new LambdaQueryWrapper<>();
        followingQuery.eq(FollowingDO::getFollowingUserId, uid)
                .eq(FollowingDO::getStatus, RecordStatus.NORMAL.code())
                .orderByDesc(FollowingDO::getUpdateTime);

        List<FollowingDO> followerRelations = followingMapper.selectList(followingQuery);
        if (followerRelations == null || followerRelations.isEmpty()) {
            return Collections.emptyList();
        }

        return buildUserCardListByRelations(followerRelations, true);
    }

    @Override
    public List<FollowersQueryVO> followingsQuery(Long uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        LambdaQueryWrapper<FollowingDO> followingQuery = new LambdaQueryWrapper<>();
        followingQuery.eq(FollowingDO::getUserId, uid)
                .eq(FollowingDO::getStatus, RecordStatus.NORMAL.code())
                .orderByDesc(FollowingDO::getUpdateTime);

        List<FollowingDO> followingRelations = followingMapper.selectList(followingQuery);
        if (followingRelations == null || followingRelations.isEmpty()) {
            return Collections.emptyList();
        }

        return buildUserCardListByRelations(followingRelations, false);
    }

    @Override
    public List<FollowersQueryVO> friendsQuery(Long uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        LambdaQueryWrapper<FollowingDO> myFollowingQuery = new LambdaQueryWrapper<>();
        myFollowingQuery.eq(FollowingDO::getUserId, uid)
                .eq(FollowingDO::getStatus, RecordStatus.NORMAL.code())
                .orderByDesc(FollowingDO::getUpdateTime);
        List<FollowingDO> myFollowingRelations = followingMapper.selectList(myFollowingQuery);
        if (myFollowingRelations == null || myFollowingRelations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> myFollowingUserIds = myFollowingRelations.stream()
                .map(FollowingDO::getFollowingUserId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<FollowingDO> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.in(FollowingDO::getUserId, myFollowingUserIds)
                .eq(FollowingDO::getFollowingUserId, uid)
                .eq(FollowingDO::getStatus, RecordStatus.NORMAL.code());
        List<FollowingDO> reverseRelations = followingMapper.selectList(reverseQuery);
        if (reverseRelations == null || reverseRelations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> friendUidSet = reverseRelations.stream()
                .map(FollowingDO::getUserId)
                .collect(Collectors.toCollection(HashSet::new));

        List<FollowingDO> friendRelations = myFollowingRelations.stream()
                .filter(item -> friendUidSet.contains(item.getFollowingUserId()))
                .collect(Collectors.toList());
        if (friendRelations.isEmpty()) {
            return Collections.emptyList();
        }

        return buildUserCardListByRelations(friendRelations, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long uid, Long targetUid) {
        if (uid == null || uid <= 0 || targetUid == null || targetUid <= 0) {
            throw new IllegalArgumentException("uid or targetUid is invalid");
        }
        if (uid.equals(targetUid)) {
            throw new IllegalArgumentException("cannot follow yourself");
        }

        ensureUserExists(uid);
        ensureUserExists(targetUid);

        LambdaQueryWrapper<FollowingDO> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(FollowingDO::getUserId, uid)
                .eq(FollowingDO::getFollowingUserId, targetUid);
        FollowingDO relation = followingMapper.selectOne(relationQuery);

        if (relation == null) {
            FollowingDO newRelation = new FollowingDO();
            newRelation.setUserId(uid);
            newRelation.setFollowingUserId(targetUid);
            newRelation.setStatus(RecordStatus.NORMAL.code());
            int insertRows = followingMapper.insert(newRelation);
            if (insertRows != 1) {
                throw new RuntimeException("insert follow relation failed");
            }
            increaseFollowStats(uid, targetUid);
            return;
        }

        if (RecordStatus.NORMAL.matches(relation.getStatus())) {
            return;
        }

        relation.setStatus(RecordStatus.NORMAL.code());
        int updateRows = followingMapper.updateById(relation);
        if (updateRows != 1) {
            throw new RuntimeException("update follow relation failed");
        }
        increaseFollowStats(uid, targetUid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long uid, Long targetUid) {
        if (uid == null || uid <= 0 || targetUid == null || targetUid <= 0) {
            throw new IllegalArgumentException("uid or targetUid is invalid");
        }
        if (uid.equals(targetUid)) {
            throw new IllegalArgumentException("cannot unfollow yourself");
        }

        ensureUserExists(uid);
        ensureUserExists(targetUid);

        LambdaUpdateWrapper<FollowingDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FollowingDO::getUserId, uid)
                .eq(FollowingDO::getFollowingUserId, targetUid)
                .eq(FollowingDO::getStatus, RecordStatus.NORMAL.code())
                .set(FollowingDO::getStatus, RecordStatus.DELETED.code());
        int updateRows = followingMapper.update(null, updateWrapper);

        // idempotent: already unfollowed / relation not found
        if (updateRows == 0) {
            return;
        }
        if (updateRows != 1) {
            throw new RuntimeException("update unfollow relation failed");
        }

        decreaseFollowStats(uid, targetUid);
    }

    private void ensureUserExists(Long uid) {
        LambdaQueryWrapper<UserDO> query = new LambdaQueryWrapper<>();
        query.eq(UserDO::getId, uid)
                .eq(UserDO::getStatus, UserStatus.NORMAL.code());
        Long count = userMapper.selectCount(query);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("user not found");
        }
    }

    private void increaseFollowStats(Long uid, Long targetUid) {
        LambdaUpdateWrapper<UserInfoDO> increaseFollowing = new LambdaUpdateWrapper<>();
        increaseFollowing.eq(UserInfoDO::getUserId, uid)
                .setSql("following_count = IFNULL(following_count, 0) + 1");
        int followingRows = userInfoMapper.update(null, increaseFollowing);
        if (followingRows != 1) {
            throw new RuntimeException("update following_count failed");
        }

        LambdaUpdateWrapper<UserInfoDO> increaseFollower = new LambdaUpdateWrapper<>();
        increaseFollower.eq(UserInfoDO::getUserId, targetUid)
                .setSql("follower_count = IFNULL(follower_count, 0) + 1");
        int followerRows = userInfoMapper.update(null, increaseFollower);
        if (followerRows != 1) {
            throw new RuntimeException("update follower_count failed");
        }
    }

    private void decreaseFollowStats(Long uid, Long targetUid) {
        LambdaUpdateWrapper<UserInfoDO> decreaseFollowing = new LambdaUpdateWrapper<>();
        decreaseFollowing.eq(UserInfoDO::getUserId, uid)
                .setSql("following_count = GREATEST(IFNULL(following_count, 0) - 1, 0)");
        int followingRows = userInfoMapper.update(null, decreaseFollowing);
        if (followingRows != 1) {
            throw new RuntimeException("update following_count failed");
        }

        LambdaUpdateWrapper<UserInfoDO> decreaseFollower = new LambdaUpdateWrapper<>();
        decreaseFollower.eq(UserInfoDO::getUserId, targetUid)
                .setSql("follower_count = GREATEST(IFNULL(follower_count, 0) - 1, 0)");
        int followerRows = userInfoMapper.update(null, decreaseFollower);
        if (followerRows != 1) {
            throw new RuntimeException("update follower_count failed");
        }
    }

    private List<FollowersQueryVO> buildUserCardListByRelations(List<FollowingDO> relations,
                                                                 boolean useInitiatorUserId) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> targetUserIds = relations.stream()
                .map(item -> useInitiatorUserId ? item.getUserId() : item.getFollowingUserId())
                .distinct()
                .collect(Collectors.toList());
        if (targetUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<UserInfoDO> userInfoQuery = new LambdaQueryWrapper<>();
        userInfoQuery.in(UserInfoDO::getUserId, targetUserIds);
        List<UserInfoDO> userInfos = userInfoMapper.selectList(userInfoQuery);

        Map<Long, UserInfoDO> userInfoMap = new HashMap<>();
        for (UserInfoDO userInfo : userInfos) {
            userInfoMap.put(userInfo.getUserId(), userInfo);
        }

        List<FollowersQueryVO> result = new ArrayList<>();
        for (FollowingDO relation : relations) {
            Long targetUid = useInitiatorUserId ? relation.getUserId() : relation.getFollowingUserId();
            UserInfoDO userInfo = userInfoMap.get(targetUid);
            if (userInfo == null) {
                continue;
            }
            FollowersQueryVO vo = new FollowersQueryVO();
            vo.setUid(userInfo.getUserId());
            vo.setNickname(userInfo.getNickname());
            vo.setAvatar(userInfo.getAvatarUrl());
            vo.setSign(userInfo.getSign());
            result.add(vo);
        }

        return result;
    }
}
