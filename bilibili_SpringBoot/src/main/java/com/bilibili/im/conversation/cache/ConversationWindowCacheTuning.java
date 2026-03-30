package com.bilibili.im.conversation.cache;

import java.time.Duration;

public final class ConversationWindowCacheTuning {

    public static final String INIT_VALUE = "1";
    public static final Duration CACHE_TTL = Duration.ofHours(12);

    private ConversationWindowCacheTuning() {
    }
}
