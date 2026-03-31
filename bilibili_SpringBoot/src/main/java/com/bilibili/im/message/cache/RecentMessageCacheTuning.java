package com.bilibili.im.message.cache;

import java.time.Duration;

public final class RecentMessageCacheTuning {

    private RecentMessageCacheTuning() {
    }

    public static final String INIT_VALUE = "1";

    public static final Duration CACHE_TTL = Duration.ofHours(12);
}
