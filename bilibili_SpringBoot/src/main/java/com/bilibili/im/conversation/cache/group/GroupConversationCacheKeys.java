package com.bilibili.im.conversation.cache.group;

public final class GroupConversationCacheKeys {

    private static final String CARD_KEY_PREFIX = "im:group:card:";

    private GroupConversationCacheKeys() {
    }

    public static String cardKey(Long groupId) {
        return CARD_KEY_PREFIX + groupId;
    }
}
