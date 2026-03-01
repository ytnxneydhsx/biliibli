package com.bilibili.config.redis;

public final class RedisViewCacheTuning {

    public static final long VIDEO_VIEW_DELTA_EXPIRE_SECONDS = 24 * 60 * 60;
    public static final int VIDEO_VIEW_RANK_WARMUP_LIMIT = 200;
    public static final long VIDEO_VIEW_SYNC_FIXED_DELAY_MS = 50000L;

    private RedisViewCacheTuning() {
    }
}
