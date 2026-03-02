package com.bilibili.config.redis;

import com.bilibili.tool.StringTool;

public final class RedisSearchKeys {

    public static final String SEARCH_HISTORY_KEY_PREFIX = "search:history";
    public static final String DOMAIN_VIDEO = "video";
    public static final String DOMAIN_USER = "user";
    public static final String DOMAIN_TAG = "tag";

    private RedisSearchKeys() {
    }

    public static String searchHistoryKey(String domain, Long uid) {
        String normalizedDomain = StringTool.normalizeRequired(domain, "domain");
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        return SEARCH_HISTORY_KEY_PREFIX + ":" + normalizedDomain.toLowerCase() + ":" + uid;
    }
}
