package com.bilibili.im.group.cache;

import com.bilibili.im.group.model.cache.ChatGroupSnapshot;

public interface ChatGroupSnapshotCache {

    ChatGroupSnapshot get(Long groupId);
}
