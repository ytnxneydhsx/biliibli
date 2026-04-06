package com.bilibili.im.message.cache.group;

public final class GroupRecentMessageCacheKeys {

    private GroupRecentMessageCacheKeys() {
    }

    public static String initKey(String conversationId) {
        return "im:group:msg:init:" + conversationId;
    }

    public static String recentKey(String conversationId) {
        return "im:group:msg:recent:" + conversationId;
    }

    public static String dataKey(String conversationId) {
        return "im:group:msg:data:" + conversationId;
    }
}
