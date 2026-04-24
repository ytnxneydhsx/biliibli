package com.bilibili.im.group.cache;

import com.bilibili.im.group.model.cache.ChatGroupMemberSnapshot;

public interface ChatGroupMemberSnapshotCache {

    ChatGroupMemberSnapshot get(Long groupId, Long userId);
}
