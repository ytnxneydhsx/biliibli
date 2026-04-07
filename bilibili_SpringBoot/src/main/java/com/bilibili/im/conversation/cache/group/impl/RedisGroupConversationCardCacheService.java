package com.bilibili.im.conversation.cache.group.impl;

import com.bilibili.im.conversation.cache.group.GroupConversationCacheKeys;
import com.bilibili.im.conversation.cache.group.GroupConversationCacheTuning;
import com.bilibili.im.conversation.cache.group.GroupConversationCardCacheService;
import com.bilibili.im.conversation.cache.group.model.GroupConversationCardCacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RedisGroupConversationCardCacheService implements GroupConversationCardCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGroupConversationCardCacheService(StringRedisTemplate stringRedisTemplate,
                                                  ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<Long, GroupConversationCardCacheValue> getGroupCards(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String cardKey = GroupConversationCacheKeys.publicCardKey();
        List<String> fields = groupIds.stream()
                .filter(groupId -> groupId != null && groupId > 0)
                .map(String::valueOf)
                .toList();
        if (fields.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> rawValues = stringRedisTemplate.opsForHash().multiGet(cardKey, new ArrayList<>(fields));
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, GroupConversationCardCacheValue> resolved = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            Object rawValue = rawValues.get(i);
            if (!(rawValue instanceof String value) || value.isBlank()) {
                continue;
            }
            GroupConversationCardCacheValue card = readCacheValue(value);
            if (!isValidCard(card)) {
                continue;
            }
            resolved.put(card.getGroupId(), card);
        }
        if (!resolved.isEmpty()) {
            stringRedisTemplate.expire(cardKey, GroupConversationCacheTuning.CACHE_TTL);
        }
        return resolved;
    }

    @Override
    public void cacheGroupCards(List<GroupConversationCardCacheValue> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        String cardKey = GroupConversationCacheKeys.publicCardKey();
        Map<String, String> payloads = new HashMap<>();
        for (GroupConversationCardCacheValue record : records) {
            if (!isValidCard(record)) {
                continue;
            }
            payloads.put(String.valueOf(record.getGroupId()), writeCacheValue(record));
        }
        if (payloads.isEmpty()) {
            return;
        }

        stringRedisTemplate.opsForHash().putAll(cardKey, payloads);
        stringRedisTemplate.expire(cardKey, GroupConversationCacheTuning.CACHE_TTL);
    }

    private boolean isValidCard(GroupConversationCardCacheValue card) {
        return card != null
                && card.getGroupId() != null
                && card.getGroupId() > 0;
    }

    private String writeCacheValue(GroupConversationCardCacheValue value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize group conversation card cache", ex);
        }
    }

    private GroupConversationCardCacheValue readCacheValue(String value) {
        try {
            return objectMapper.readValue(value, GroupConversationCardCacheValue.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
