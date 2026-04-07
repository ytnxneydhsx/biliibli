package com.bilibili.im.conversation.cache.group;

public final class GroupConversationCacheKeys {

    private static final String PUBLIC_CARD_KEY = "im:group:card";

    private GroupConversationCacheKeys() {
    }

    public static String publicCardKey() {
        return PUBLIC_CARD_KEY;
    }
}
