package com.bilibili.im.contact.cache;

public final class ContactRelationCacheKeys {

    private static final String DM_CONTACT_KEY_PREFIX = "im:contact:dm:";

    private ContactRelationCacheKeys() {
    }

    public static String dmContactKey(Long ownerUserId, Long targetUserId) {
        return DM_CONTACT_KEY_PREFIX + ownerUserId + ":" + targetUserId;
    }
}
