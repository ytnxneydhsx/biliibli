package com.bilibili.config.redis;

public final class RedisSearchCacheTuning {

    public static final int SEARCH_HISTORY_MAX_SIZE = 10;
    public static final long SEARCH_HISTORY_TTL_HOURS = 1L;

    private RedisSearchCacheTuning() {
    }
}
