package com.bilibili.im.message.cache;

public final class RecentMessageCacheKeys {

    private RecentMessageCacheKeys() {
    }

    public static String initKey(String conversationId) {
        return "im:msg:init:" + conversationId;
    }

    public static String recentKey(String conversationId) {
        return "im:msg:recent:" + conversationId;
    }

    public static String dataKey(String conversationId) {
        return "im:msg:data:" + conversationId;
    }
}
