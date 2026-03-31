package com.bilibili.im.message.cache.impl;

import com.bilibili.im.message.cache.RecentMessageCacheKeys;
import com.bilibili.im.message.cache.RecentMessageCacheService;
import com.bilibili.im.message.cache.RecentMessageCacheTuning;
import com.bilibili.im.message.cache.MessageCacheTuning;
import com.bilibili.im.message.cache.model.RecentMessageCacheValue;
import com.bilibili.im.message.model.vo.MessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.ZoneOffset;

@Service
public class RedisRecentMessageCacheService implements RecentMessageCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRecentMessageCacheService(StringRedisTemplate stringRedisTemplate,
                                          ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MessageVO> listRecentMessages(String conversationId, int limit) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit is invalid");
        }

        String initKey = RecentMessageCacheKeys.initKey(conversationId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(initKey))) {
            return null;
        }

        String recentKey = RecentMessageCacheKeys.recentKey(conversationId);
        String dataKey = RecentMessageCacheKeys.dataKey(conversationId);
        Set<String> messageIds = stringRedisTemplate.opsForZSet().reverseRange(recentKey, 0, limit - 1L);
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderedIds = new ArrayList<>(messageIds);
        List<Object> rawValues = stringRedisTemplate.opsForHash().multiGet(dataKey, new ArrayList<>(orderedIds));
        if (rawValues == null || rawValues.size() != orderedIds.size()) {
            return null;
        }

        Map<String, MessageVO> resolved = new LinkedHashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Object rawValue = rawValues.get(i);
            if (!(rawValue instanceof String value) || value.isBlank()) {
                return null;
            }
            MessageVO message = toMessageVO(readCacheValue(value));
            if (message == null || message.getServerMessageId() == null) {
                return null;
            }
            resolved.put(orderedIds.get(i), message);
        }

        List<MessageVO> records = new ArrayList<>(orderedIds.size());
        for (String orderedId : orderedIds) {
            MessageVO message = resolved.get(orderedId);
            if (message == null) {
                return null;
            }
            records.add(message);
        }
        Collections.reverse(records);
        refreshTtl(conversationId);
        return records;
    }

    @Override
    public void initializeRecentMessages(String conversationId, List<MessageVO> records) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }

        String recentKey = RecentMessageCacheKeys.recentKey(conversationId);
        String dataKey = RecentMessageCacheKeys.dataKey(conversationId);
        String initKey = RecentMessageCacheKeys.initKey(conversationId);

        if (records != null && !records.isEmpty()) {
            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            Map<String, String> payloads = new HashMap<>();
            for (MessageVO record : records) {
                if (!isValidMessage(record)) {
                    continue;
                }
                String serverMessageId = String.valueOf(record.getServerMessageId());
                tuples.add(new DefaultTypedTuple<>(serverMessageId, toScore(record)));
                payloads.put(serverMessageId, writeCacheValue(toCacheValue(record)));
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(recentKey, tuples);
            }
            if (!payloads.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(dataKey, payloads);
            }
            trimOverflow(recentKey, dataKey);
        }

        stringRedisTemplate.opsForValue().set(
                initKey,
                RecentMessageCacheTuning.INIT_VALUE,
                RecentMessageCacheTuning.CACHE_TTL
        );
        refreshTtl(conversationId);
    }

    @Override
    public void appendMessageIfInitialized(String conversationId, MessageVO record) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }
        if (!isValidMessage(record)) {
            throw new IllegalArgumentException("record is invalid");
        }

        String initKey = RecentMessageCacheKeys.initKey(conversationId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(initKey))) {
            return;
        }

        String recentKey = RecentMessageCacheKeys.recentKey(conversationId);
        String dataKey = RecentMessageCacheKeys.dataKey(conversationId);
        String serverMessageId = String.valueOf(record.getServerMessageId());

        stringRedisTemplate.opsForZSet().add(recentKey, serverMessageId, toScore(record));
        stringRedisTemplate.opsForHash().put(dataKey, serverMessageId, writeCacheValue(toCacheValue(record)));
        trimOverflow(recentKey, dataKey);
        refreshTtl(conversationId);
    }

    private void trimOverflow(String recentKey, String dataKey) {
        Long size = stringRedisTemplate.opsForZSet().zCard(recentKey);
        if (size == null || size <= MessageCacheTuning.RECENT_MESSAGE_CACHE_LIMIT) {
            return;
        }
        long removeCount = size - MessageCacheTuning.RECENT_MESSAGE_CACHE_LIMIT;
        Set<String> expiredIds = stringRedisTemplate.opsForZSet().range(recentKey, 0, removeCount - 1L);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return;
        }
        stringRedisTemplate.opsForZSet().remove(recentKey, expiredIds.toArray());
        stringRedisTemplate.opsForHash().delete(dataKey, expiredIds.toArray());
    }

    private void refreshTtl(String conversationId) {
        stringRedisTemplate.expire(RecentMessageCacheKeys.initKey(conversationId), RecentMessageCacheTuning.CACHE_TTL);
        stringRedisTemplate.expire(RecentMessageCacheKeys.recentKey(conversationId), RecentMessageCacheTuning.CACHE_TTL);
        stringRedisTemplate.expire(RecentMessageCacheKeys.dataKey(conversationId), RecentMessageCacheTuning.CACHE_TTL);
    }

    private boolean isValidMessage(MessageVO record) {
        return record != null
                && record.getServerMessageId() != null
                && record.getConversationId() != null
                && !record.getConversationId().isBlank()
                && record.getSendTime() != null;
    }

    private double toScore(MessageVO record) {
        return record.getSendTime().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private String writeCacheValue(RecentMessageCacheValue value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize recent message cache", ex);
        }
    }

    private RecentMessageCacheValue readCacheValue(String value) {
        try {
            return objectMapper.readValue(value, RecentMessageCacheValue.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private RecentMessageCacheValue toCacheValue(MessageVO record) {
        if (record == null) {
            return null;
        }
        RecentMessageCacheValue value = new RecentMessageCacheValue();
        value.setId(record.getId());
        value.setServerMessageId(record.getServerMessageId());
        value.setConversationId(record.getConversationId());
        value.setSenderId(record.getSenderId());
        value.setReceiverId(record.getReceiverId());
        value.setClientMessageId(record.getClientMessageId());
        value.setSenderLocation(record.getSenderLocation());
        value.setMessageType(record.getMessageType());
        value.setContent(record.getContent());
        value.setSendTime(record.getSendTime());
        value.setStatus(record.getStatus());
        return value;
    }

    private MessageVO toMessageVO(RecentMessageCacheValue value) {
        if (value == null) {
            return null;
        }
        MessageVO message = new MessageVO();
        message.setId(value.getId());
        message.setServerMessageId(value.getServerMessageId());
        message.setConversationId(value.getConversationId());
        message.setSenderId(value.getSenderId());
        message.setReceiverId(value.getReceiverId());
        message.setClientMessageId(value.getClientMessageId());
        message.setSenderLocation(value.getSenderLocation());
        message.setMessageType(value.getMessageType());
        message.setContent(value.getContent());
        message.setSendTime(value.getSendTime());
        message.setStatus(value.getStatus());
        return message;
    }
}
