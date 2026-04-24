package com.bilibili.im.contact.cache;

public final class ContactRelationCacheKeys {

    private ContactRelationCacheKeys() {
    }

    public static String relationKey(Long ownerUserId, Long targetUserId) {
        return ownerUserId + ":" + targetUserId;
    }
}
