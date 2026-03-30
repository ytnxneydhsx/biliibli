package com.bilibili.im.conversation.cache;

public final class ConversationWindowCacheKeys {

    private static final String LIST_KEY_PREFIX = "im:conv:list:";
    private static final String META_KEY_PREFIX = "im:conv:meta:";
    private static final String INIT_KEY_PREFIX = "im:conv:init:";

    private ConversationWindowCacheKeys() {
    }

    public static String listKey(Long ownerUserId) {
        return LIST_KEY_PREFIX + ownerUserId;
    }

    public static String metaKey(Long ownerUserId) {
        return META_KEY_PREFIX + ownerUserId;
    }

    public static String initKey(Long ownerUserId) {
        return INIT_KEY_PREFIX + ownerUserId;
    }
}
