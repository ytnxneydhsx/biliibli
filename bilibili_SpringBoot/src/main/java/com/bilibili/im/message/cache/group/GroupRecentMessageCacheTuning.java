package com.bilibili.im.message.cache.group;

import java.time.Duration;

public final class GroupRecentMessageCacheTuning {

    private GroupRecentMessageCacheTuning() {
    }

    public static final String INIT_VALUE = "1";

    public static final Duration CACHE_TTL = Duration.ofHours(12);
}
