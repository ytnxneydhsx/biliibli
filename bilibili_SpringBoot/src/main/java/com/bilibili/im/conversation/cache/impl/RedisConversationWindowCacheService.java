package com.bilibili.im.conversation.cache.impl;

import com.bilibili.im.conversation.ConversationWindowTuning;
import com.bilibili.im.conversation.cache.ConversationWindowCacheKeys;
import com.bilibili.im.conversation.cache.ConversationWindowCacheService;
import com.bilibili.im.conversation.cache.ConversationWindowCacheTuning;
import com.bilibili.im.conversation.cache.model.ConversationWindowCacheValue;
import com.bilibili.im.conversation.model.vo.ConversationWindowVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisConversationWindowCacheService implements ConversationWindowCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisConversationWindowCacheService(StringRedisTemplate stringRedisTemplate,
                                               ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isInitialized(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ConversationWindowCacheKeys.initKey(ownerUserId)));
    }

    @Override
    public List<ConversationWindowVO> listRecentConversations(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        int safeLimit = ConversationWindowTuning.RECENT_WINDOW_LIMIT;

        String listKey = ConversationWindowCacheKeys.listKey(ownerUserId);
        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        String initKey = ConversationWindowCacheKeys.initKey(ownerUserId);

        if (!isInitialized(ownerUserId)) {
            return null;
        }

        Set<String> conversationIds = stringRedisTemplate.opsForZSet().reverseRange(listKey, 0, safeLimit - 1L);
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderedIds = new ArrayList<>(conversationIds);
        List<Object> rawValues = stringRedisTemplate.opsForHash().multiGet(metaKey, new ArrayList<>(orderedIds));
        if (rawValues == null || rawValues.size() != orderedIds.size()) {
            return null;
        }

        Map<String, ConversationWindowVO> resolved = new LinkedHashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Object rawValue = rawValues.get(i);
            if (!(rawValue instanceof String value) || value.isBlank()) {
                return null;
            }
            ConversationWindowVO window = toConversationWindowVO(readWindowValue(value));
            if (window == null || window.getConversationId() == null || window.getConversationId().isBlank()) {
                return null;
            }
            resolved.put(orderedIds.get(i), window);
        }

        List<ConversationWindowVO> records = new ArrayList<>(orderedIds.size());
        for (String orderedId : orderedIds) {
            ConversationWindowVO window = resolved.get(orderedId);
            if (window == null) {
                return null;
            }
            records.add(window);
        }
        refreshTtl(listKey, metaKey, initKey);
        return records;
    }

    @Override
    public void replaceRecentConversations(Long ownerUserId, List<ConversationWindowVO> records) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }

        String listKey = ConversationWindowCacheKeys.listKey(ownerUserId);
        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        String initKey = ConversationWindowCacheKeys.initKey(ownerUserId);

        stringRedisTemplate.delete(List.of(listKey, metaKey, initKey));
        stringRedisTemplate.opsForValue().set(
                initKey,
                ConversationWindowCacheTuning.INIT_VALUE,
                ConversationWindowCacheTuning.CACHE_TTL
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (ConversationWindowVO record : records) {
            if (!isValidWindow(record)) {
                continue;
            }
            stringRedisTemplate.opsForZSet().add(listKey, record.getConversationId(), toScore(record));
            stringRedisTemplate.opsForHash().put(metaKey, record.getConversationId(), writeWindow(toCacheValue(record)));
        }
        trimOverflow(listKey, metaKey);
        refreshTtl(listKey, metaKey, initKey);
    }

    @Override
    public void cacheConversationWindow(Long ownerUserId, ConversationWindowVO window) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (!isValidWindow(window)) {
            return;
        }

        String listKey = ConversationWindowCacheKeys.listKey(ownerUserId);
        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        String initKey = ConversationWindowCacheKeys.initKey(ownerUserId);

        stringRedisTemplate.opsForZSet().add(listKey, window.getConversationId(), toScore(window));
        stringRedisTemplate.opsForHash().put(metaKey, window.getConversationId(), writeWindow(toCacheValue(window)));
        trimOverflow(listKey, metaKey);
        refreshTtl(listKey, metaKey, initKey);
    }

    @Override
    public ConversationWindowCacheValue getConversationWindow(Long ownerUserId, String conversationId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }

        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        Object rawValue = stringRedisTemplate.opsForHash().get(metaKey, conversationId);
        if (!(rawValue instanceof String value) || value.isBlank()) {
            return null;
        }

        ConversationWindowCacheValue window = readWindowValue(value);
        if (window == null || window.getConversationId() == null || window.getConversationId().isBlank()) {
            return null;
        }
        return window;
    }

    @Override
    public void cacheConversationWindowValue(Long ownerUserId, ConversationWindowCacheValue window) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (!isValidWindow(window)) {
            return;
        }

        String listKey = ConversationWindowCacheKeys.listKey(ownerUserId);
        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        String initKey = ConversationWindowCacheKeys.initKey(ownerUserId);

        stringRedisTemplate.opsForZSet().add(listKey, window.getConversationId(), toScore(window));
        stringRedisTemplate.opsForHash().put(metaKey, window.getConversationId(), writeWindow(window));
        trimOverflow(listKey, metaKey);
        refreshTtl(listKey, metaKey, initKey);
    }

    private void trimOverflow(String listKey, String metaKey) {
        Long size = stringRedisTemplate.opsForZSet().zCard(listKey);
        if (size == null || size <= ConversationWindowTuning.RECENT_WINDOW_LIMIT) {
            return;
        }
        long removeCount = size - ConversationWindowTuning.RECENT_WINDOW_LIMIT;
        Set<String> expiredIds = stringRedisTemplate.opsForZSet().range(listKey, 0, removeCount - 1L);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return;
        }
        stringRedisTemplate.opsForZSet().remove(listKey, expiredIds.toArray());
        stringRedisTemplate.opsForHash().delete(metaKey, expiredIds.toArray());
    }

    private void refreshTtl(String listKey, String metaKey, String initKey) {
        stringRedisTemplate.expire(listKey, ConversationWindowCacheTuning.CACHE_TTL);
        stringRedisTemplate.expire(metaKey, ConversationWindowCacheTuning.CACHE_TTL);
        stringRedisTemplate.expire(initKey, ConversationWindowCacheTuning.CACHE_TTL);
    }

    private double toScore(ConversationWindowVO window) {
        if (window == null || window.getLastMessageTime() == null) {
            return 0D;
        }
        return window.getLastMessageTime().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private double toScore(ConversationWindowCacheValue window) {
        if (window == null || window.getLastMessageTime() == null) {
            return 0D;
        }
        return window.getLastMessageTime().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private boolean isValidWindow(ConversationWindowVO window) {
        return window != null
                && window.getConversationId() != null
                && !window.getConversationId().isBlank()
                && window.getTargetId() != null
                && window.getTargetId() > 0;
    }

    private boolean isValidWindow(ConversationWindowCacheValue window) {
        return window != null
                && window.getConversationId() != null
                && !window.getConversationId().isBlank()
                && window.getTargetId() != null
                && window.getTargetId() > 0;
    }

    private String writeWindow(ConversationWindowCacheValue window) {
        try {
            return objectMapper.writeValueAsString(window);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize conversation window cache", ex);
        }
    }

    private ConversationWindowCacheValue readWindowValue(String value) {
        try {
            return objectMapper.readValue(value, ConversationWindowCacheValue.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private ConversationWindowCacheValue toCacheValue(ConversationWindowVO window) {
        if (window == null) {
            return null;
        }
        ConversationWindowCacheValue value = new ConversationWindowCacheValue();
        value.setConversationId(window.getConversationId());
        value.setTargetId(window.getTargetId());
        value.setLastMessage(window.getLastMessage());
        value.setLastMessageTime(window.getLastMessageTime());
        value.setUnreadCount(window.getUnreadCount());
        value.setIsMuted(window.getIsMuted());
        return value;
    }

    private ConversationWindowVO toConversationWindowVO(ConversationWindowCacheValue value) {
        if (value == null) {
            return null;
        }
        ConversationWindowVO window = new ConversationWindowVO();
        window.setConversationId(value.getConversationId());
        window.setTargetId(value.getTargetId());
        window.setLastMessage(value.getLastMessage());
        window.setLastMessageTime(value.getLastMessageTime());
        window.setUnreadCount(value.getUnreadCount());
        window.setIsMuted(value.getIsMuted());
        return window;
    }
}
