package com.bilibili.im.contact.model.cache;

import java.io.Serializable;

public record ContactRelationSnapshot(
        Long ownerUserId,
        Long targetUserId,
        boolean exists,
        boolean contact,
        boolean dmContact,
        boolean blocked,
        boolean muted
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static ContactRelationSnapshot notFound(Long ownerUserId, Long targetUserId) {
        return new ContactRelationSnapshot(ownerUserId, targetUserId, false, false, false, false, false);
    }
}
