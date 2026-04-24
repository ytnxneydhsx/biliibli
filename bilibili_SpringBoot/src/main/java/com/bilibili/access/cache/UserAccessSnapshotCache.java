package com.bilibili.access.cache;

import com.bilibili.access.model.cache.UserAccessSnapshot;

public interface UserAccessSnapshotCache {

    UserAccessSnapshot get(Long userId);
}
