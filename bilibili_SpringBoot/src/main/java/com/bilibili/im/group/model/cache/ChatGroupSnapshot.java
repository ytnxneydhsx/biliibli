package com.bilibili.im.group.model.cache;

import java.io.Serializable;

public record ChatGroupSnapshot(
        Long groupId,
        boolean exists,
        Long ownerUserId,
        Integer status,
        boolean allMuted
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static ChatGroupSnapshot notFound(Long groupId) {
        return new ChatGroupSnapshot(groupId, false, null, null, false);
    }
}
