package com.bilibili.access.model.cache;

import java.io.Serializable;

public record UserAccessSnapshot(
        Long userId,
        boolean likeEnabled,
        boolean commentEnabled,
        boolean imMessageSendEnabled,
        boolean videoUploadEnabled,
        boolean profileEditEnabled
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static UserAccessSnapshot defaults(Long userId) {
        return new UserAccessSnapshot(userId, true, true, true, true, true);
    }
}
