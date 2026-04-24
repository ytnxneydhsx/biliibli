package com.bilibili.im.group.cache.impl;

import com.bilibili.im.group.cache.ChatGroupMemberSnapshotCache;
import com.bilibili.im.group.cache.GroupPermissionCacheEvictor;
import com.bilibili.im.group.cache.GroupPermissionCacheNames;
import com.bilibili.im.group.mapper.ChatGroupMemberMapper;
import com.bilibili.im.group.model.cache.ChatGroupMemberSnapshot;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class SpringChatGroupMemberSnapshotCache implements ChatGroupMemberSnapshotCache {

    private static final int TRUE_VALUE = 1;

    private final ChatGroupMemberMapper chatGroupMemberMapper;

    public SpringChatGroupMemberSnapshotCache(ChatGroupMemberMapper chatGroupMemberMapper) {
        this.chatGroupMemberMapper = chatGroupMemberMapper;
    }

    @Override
    @Cacheable(cacheNames = GroupPermissionCacheNames.GROUP_MEMBER_SNAPSHOT,
            key = "T(com.bilibili.im.group.cache.GroupPermissionCacheEvictor).groupMemberKey(#p0, #p1)")
    public ChatGroupMemberSnapshot get(Long groupId, Long userId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }

        ChatGroupMemberDO membership = chatGroupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (membership == null) {
            return ChatGroupMemberSnapshot.notFound(groupId, userId);
        }

        return new ChatGroupMemberSnapshot(
                groupId,
                userId,
                true,
                membership.getRole(),
                membership.getStatus(),
                isTrue(membership.getIsMuted())
        );
    }

    private boolean isTrue(Integer value) {
        return value != null && value == TRUE_VALUE;
    }
}
