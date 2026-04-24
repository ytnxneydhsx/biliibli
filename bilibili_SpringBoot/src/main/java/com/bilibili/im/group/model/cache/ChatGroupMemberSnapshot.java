package com.bilibili.im.group.model.cache;

import java.io.Serializable;

public record ChatGroupMemberSnapshot(
        Long groupId,
        Long userId,
        boolean exists,
        Integer role,
        Integer status,
        boolean muted
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static ChatGroupMemberSnapshot notFound(Long groupId, Long userId) {
        return new ChatGroupMemberSnapshot(groupId, userId, false, null, null, false);
    }
}
