package com.bilibili.im.group.cache.impl;

import com.bilibili.im.group.cache.ChatGroupSnapshotCache;
import com.bilibili.im.group.cache.GroupPermissionCacheNames;
import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.model.cache.ChatGroupSnapshot;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class SpringChatGroupSnapshotCache implements ChatGroupSnapshotCache {

    private static final int TRUE_VALUE = 1;

    private final ChatGroupMapper chatGroupMapper;

    public SpringChatGroupSnapshotCache(ChatGroupMapper chatGroupMapper) {
        this.chatGroupMapper = chatGroupMapper;
    }

    @Override
    @Cacheable(cacheNames = GroupPermissionCacheNames.GROUP_SNAPSHOT, key = "#p0")
    public ChatGroupSnapshot get(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            return ChatGroupSnapshot.notFound(groupId);
        }

        return new ChatGroupSnapshot(
                groupId,
                true,
                group.getOwnerUserId(),
                group.getStatus(),
                isTrue(group.getIsAllMuted())
        );
    }

    private boolean isTrue(Integer value) {
        return value != null && value == TRUE_VALUE;
    }
}
