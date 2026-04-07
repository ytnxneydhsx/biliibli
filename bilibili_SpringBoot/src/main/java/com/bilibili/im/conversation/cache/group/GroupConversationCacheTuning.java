package com.bilibili.im.conversation.cache.group;

import java.time.Duration;

public final class GroupConversationCacheTuning {

    public static final int PAGE_SIZE = 50;
    public static final Duration CACHE_TTL = Duration.ofDays(1);

    private GroupConversationCacheTuning() {
    }
}
