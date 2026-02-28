package com.bilibili.config.redis;

public final class RedisViewCacheKeys {

    public static final String VIDEO_VIEW_RANK_KEY = "rank:video:view";
    public static final String VIDEO_VIEW_DELTA_KEY_PREFIX = "video:view:delta:";
    public static final String VIDEO_VIEW_DIRTY_KEY = "video:view:dirty";


    private RedisViewCacheKeys() {
    }

    public static String buildVideoViewDeltaKey(Long videoId) {
        return VIDEO_VIEW_DELTA_KEY_PREFIX + videoId;
    }
}
