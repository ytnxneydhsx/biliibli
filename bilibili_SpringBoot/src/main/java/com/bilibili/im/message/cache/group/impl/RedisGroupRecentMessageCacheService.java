package com.bilibili.im.message.cache.group.impl;

import com.bilibili.im.message.cache.MessageCacheTuning;
import com.bilibili.im.message.cache.group.GroupRecentMessageCacheKeys;
import com.bilibili.im.message.cache.group.GroupRecentMessageCacheService;
import com.bilibili.im.message.cache.group.GroupRecentMessageCacheTuning;
import com.bilibili.im.message.cache.group.model.GroupRecentMessageCacheValue;
import com.bilibili.im.message.model.vo.MessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisGroupRecentMessageCacheService implements GroupRecentMessageCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGroupRecentMessageCacheService(StringRedisTemplate stringRedisTemplate,
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

        String initKey = GroupRecentMessageCacheKeys.initKey(conversationId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(initKey))) {
            return null;
        }

        String recentKey = GroupRecentMessageCacheKeys.recentKey(conversationId);
        String dataKey = GroupRecentMessageCacheKeys.dataKey(conversationId);
        Set<String> messageIds = stringRedisTemplate.opsForZSet().reverseRange(recentKey, 0, limit - 1L);
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderedIds = new ArrayList<>(messageIds);
        List<Object> rawValues = stringRedisTemplate.opsForHash().multiGet(dataKey, new ArrayList<>(orderedIds));
        if (rawValues == null || rawValues.size() != orderedIds.size()) {
            return null;
        }

        List<MessageVO> records = new ArrayList<>(orderedIds.size());
        for (int i = 0; i < orderedIds.size(); i++) {
            Object rawValue = rawValues.get(i);
            if (!(rawValue instanceof String value) || value.isBlank()) {
                continue;
            }
            MessageVO message = toMessageVO(readCacheValue(value));
            if (message == null || message.getServerMessageId() == null) {
                continue;
            }
            records.add(message);
        }
        Collections.reverse(records);
        return records;
    }

    @Override
    public void initializeRecentMessages(String conversationId, List<MessageVO> records) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }

        String recentKey = GroupRecentMessageCacheKeys.recentKey(conversationId);
        String dataKey = GroupRecentMessageCacheKeys.dataKey(conversationId);
        String initKey = GroupRecentMessageCacheKeys.initKey(conversationId);

        stringRedisTemplate.delete(List.of(recentKey, dataKey, initKey));

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
                GroupRecentMessageCacheTuning.INIT_VALUE,
                GroupRecentMessageCacheTuning.CACHE_TTL
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

        String initKey = GroupRecentMessageCacheKeys.initKey(conversationId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(initKey))) {
            return;
        }

        String recentKey = GroupRecentMessageCacheKeys.recentKey(conversationId);
        String dataKey = GroupRecentMessageCacheKeys.dataKey(conversationId);
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
        stringRedisTemplate.expire(GroupRecentMessageCacheKeys.initKey(conversationId), GroupRecentMessageCacheTuning.CACHE_TTL);
        stringRedisTemplate.expire(GroupRecentMessageCacheKeys.recentKey(conversationId), GroupRecentMessageCacheTuning.CACHE_TTL);
        stringRedisTemplate.expire(GroupRecentMessageCacheKeys.dataKey(conversationId), GroupRecentMessageCacheTuning.CACHE_TTL);
    }

    private boolean isValidMessage(MessageVO record) {
        return record != null
                && record.getServerMessageId() != null
                && record.getConversationId() != null
                && !record.getConversationId().isBlank()
                && record.getSendTime() != null;
    }

    private double toScore(MessageVO record) {
        return record.getSendTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    private String writeCacheValue(GroupRecentMessageCacheValue value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize group recent message cache", ex);
        }
    }

    private GroupRecentMessageCacheValue readCacheValue(String value) {
        try {
            return objectMapper.readValue(value, GroupRecentMessageCacheValue.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private GroupRecentMessageCacheValue toCacheValue(MessageVO record) {
        if (record == null) {
            return null;
        }
        GroupRecentMessageCacheValue value = new GroupRecentMessageCacheValue();
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

    private MessageVO toMessageVO(GroupRecentMessageCacheValue value) {
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
