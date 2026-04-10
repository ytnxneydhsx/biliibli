package com.bilibili.im.conversation.cache.group.impl;

import com.bilibili.im.conversation.cache.group.GroupConversationCacheKeys;
import com.bilibili.im.conversation.cache.group.GroupConversationCacheTuning;
import com.bilibili.im.conversation.cache.group.GroupConversationCardCacheService;
import com.bilibili.im.conversation.cache.group.model.GroupConversationCardCacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    public GroupConversationCardCacheValue getGroupCard(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        String key = GroupConversationCacheKeys.cardKey(groupId);
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        GroupConversationCardCacheValue card = readCacheValue(raw);
        if (!isValidCard(card)) {
            return null;
        }
        return card;
    }

    @Override
    public Map<Long, GroupConversationCardCacheValue> getGroupCards(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> validGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && groupId > 0)
                .toList();
        if (validGroupIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = validGroupIds.stream()
                .map(GroupConversationCacheKeys::cardKey)
                .toList();

        List<String> rawValues = stringRedisTemplate.opsForValue().multiGet(keys);
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, GroupConversationCardCacheValue> resolved = new HashMap<>();
        for (int i = 0; i < validGroupIds.size(); i++) {
            String raw = rawValues.get(i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            GroupConversationCardCacheValue card = readCacheValue(raw);
            if (!isValidCard(card)) {
                continue;
            }
            resolved.put(card.getGroupId(), card);
        }
        return resolved;
    }

    @Override
    public void cacheGroupCard(GroupConversationCardCacheValue record) {
        if (!isValidCard(record)) {
            return;
        }
        String key = GroupConversationCacheKeys.cardKey(record.getGroupId());
        stringRedisTemplate.opsForValue().set(key, writeCacheValue(record), GroupConversationCacheTuning.CACHE_TTL);
    }

    @Override
    public void cacheGroupCards(List<GroupConversationCardCacheValue> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (GroupConversationCardCacheValue record : records) {
            cacheGroupCard(record);
        }
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
