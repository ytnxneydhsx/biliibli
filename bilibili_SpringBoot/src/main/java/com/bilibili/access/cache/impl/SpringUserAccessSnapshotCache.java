package com.bilibili.access.cache.impl;

import com.bilibili.access.cache.AccessCacheNames;
import com.bilibili.access.cache.UserAccessSnapshotCache;
import com.bilibili.access.mapper.UserAccessMapper;
import com.bilibili.access.model.cache.UserAccessSnapshot;
import com.bilibili.access.model.entity.UserAccessDO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class SpringUserAccessSnapshotCache implements UserAccessSnapshotCache {

    private static final int ENABLED = 1;

    private final UserAccessMapper userAccessMapper;

    public SpringUserAccessSnapshotCache(UserAccessMapper userAccessMapper) {
        this.userAccessMapper = userAccessMapper;
    }

    @Override
    @Cacheable(cacheNames = AccessCacheNames.USER_ACCESS_SNAPSHOT, key = "#p0")
    public UserAccessSnapshot get(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }

        UserAccessDO userAccess = userAccessMapper.selectByUserId(userId);
        if (userAccess == null) {
            return UserAccessSnapshot.defaults(userId);
        }

        return new UserAccessSnapshot(
                userId,
                isEnabled(userAccess.getLikeEnabled()),
                isEnabled(userAccess.getCommentEnabled()),
                isEnabled(userAccess.getImMessageSendEnabled()),
                isEnabled(userAccess.getVideoUploadEnabled()),
                isEnabled(userAccess.getProfileEditEnabled())
        );
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == ENABLED;
    }
}
