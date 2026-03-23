package com.bilibili.video.redis.key;

public final class VideoRedisKeys {

    public static final String ACTIVE_SLOT_KEY = "home:video:active-slot";
    public static final String WRITE_FROZEN_KEY = "home:video:write-frozen";

    private static final String RANK_PREFIX = "rank:video:view:";
    private static final String CARD_PREFIX = "video:card:";
    private static final String DIRTY_PREFIX = "video:dirty:";
    private static final String CARD_INDEX_PREFIX = "video:card:index:";

    private VideoRedisKeys() {
    }

    public static String rankKey(String slot) {
        return RANK_PREFIX + slot;
    }

    public static String cardKeyPrefix(String slot) {
        return CARD_PREFIX + slot + ":";
    }

    public static String cardKey(String slot, Long videoId) {
        return cardKeyPrefix(slot) + videoId;
    }

    public static String dirtyKey(String slot) {
        return DIRTY_PREFIX + slot;
    }

    public static String cardIndexKey(String slot) {
        return CARD_INDEX_PREFIX + slot;
    }
}
