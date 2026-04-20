package com.bilibili.im.conversation.cache.impl;

import com.bilibili.im.conversation.ConversationWindowTuning;
import com.bilibili.im.conversation.cache.ConversationWindowCacheKeys;
import com.bilibili.im.conversation.cache.ConversationWindowCacheService;
import com.bilibili.im.conversation.cache.ConversationWindowCacheTuning;
import com.bilibili.im.conversation.cache.model.ConversationWindowCacheValue;
import com.bilibili.im.conversation.model.vo.ConversationWindowVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
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
public class RedisConversationWindowCacheService implements ConversationWindowCacheService {

    private static final DefaultRedisScript<String> PROJECT_WINDOW_EVENT_SCRIPT = projectWindowEventScript();
    private static final DefaultRedisScript<String> CACHE_BASELINE_IF_ABSENT_SCRIPT = cacheBaselineIfAbsentScript();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static DefaultRedisScript<String> projectWindowEventScript() {
        return loadStringScript("scripts/redis/project_conversation_window_event.lua");
    }

    private static DefaultRedisScript<String> cacheBaselineIfAbsentScript() {
        return loadStringScript("scripts/redis/cache_conversation_window_baseline_if_absent.lua");
    }

    private static DefaultRedisScript<String> loadStringScript(String path) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(String.class);
        return script;
    }

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

        List<ConversationWindowVO> records = new ArrayList<>(orderedIds.size());
        for (int i = 0; i < orderedIds.size(); i++) {
            Object rawValue = rawValues.get(i);
            if (!(rawValue instanceof String value) || value.isBlank()) {
                continue;
            }
            ConversationWindowVO window = toConversationWindowVO(readWindowValue(value));
            if (window == null || window.getConversationId() == null || window.getConversationId().isBlank()) {
                continue;
            }
            records.add(window);
        }
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
        Duration ttl = ConversationWindowCacheTuning.CACHE_TTL;

        int limit = ConversationWindowTuning.RECENT_WINDOW_LIMIT;
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        Map<String, String> payloads = new HashMap<>();

        if (records != null) {
            List<ConversationWindowVO> bounded = records.size() <= limit ? records : records.subList(0, limit);
            for (ConversationWindowVO record : bounded) {
                if (!isValidWindow(record)) {
                    continue;
                }
                tuples.add(new DefaultTypedTuple<>(record.getConversationId(), toScore(record)));
                payloads.put(record.getConversationId(), writeWindow(prepareSnapshot(toCacheValue(record))));
            }
        }

        stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                operations.delete(List.of(listKey, metaKey, initKey));
                operations.opsForValue().set(initKey, ConversationWindowCacheTuning.INIT_VALUE, ttl);
                if (!tuples.isEmpty()) {
                    operations.opsForZSet().add(listKey, tuples);
                    operations.expire(listKey, ttl);
                }
                if (!payloads.isEmpty()) {
                    operations.opsForHash().putAll(metaKey, payloads);
                    operations.expire(metaKey, ttl);
                }
                return null;
            }
        });
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
        stringRedisTemplate.opsForHash().put(metaKey, window.getConversationId(), writeWindow(prepareSnapshot(toCacheValue(window))));
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
        stringRedisTemplate.opsForHash().put(metaKey, window.getConversationId(), writeWindow(prepareSnapshot(window)));
        trimOverflow(listKey, metaKey);
        refreshTtl(listKey, metaKey, initKey);
    }

    @Override
    public ConversationWindowCacheValue cacheConversationWindowBaselineIfAbsent(Long ownerUserId,
                                                                               ConversationWindowCacheValue window) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (!isValidWindow(window)) {
            return null;
        }

        ConversationWindowCacheValue baseline = prepareSnapshot(window);
        String listKey = ConversationWindowCacheKeys.listKey(ownerUserId);
        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        String initKey = ConversationWindowCacheKeys.initKey(ownerUserId);
        String result = stringRedisTemplate.execute(
                CACHE_BASELINE_IF_ABSENT_SCRIPT,
                List.of(listKey, metaKey, initKey),
                baseline.getConversationId(),
                writeWindow(baseline),
                String.valueOf(toScore(baseline)),
                String.valueOf(ConversationWindowCacheTuning.CACHE_TTL.toSeconds())
        );
        if (result == null || result.isBlank()) {
            return null;
        }
        trimOverflow(listKey, metaKey);
        return readWindowValue(result);
    }

    @Override
    public ConversationWindowCacheValue projectConversationWindowEvent(Long ownerUserId,
                                                                       String conversationId,
                                                                       Long targetId,
                                                                       String lastMessage,
                                                                       LocalDateTime lastMessageTime,
                                                                       Long lastServerMessageId,
                                                                       boolean incrementUnread) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }
        if (targetId == null || targetId <= 0) {
            throw new IllegalArgumentException("targetId is invalid");
        }
        if (lastMessageTime == null) {
            throw new IllegalArgumentException("lastMessageTime is invalid");
        }
        if (lastServerMessageId == null || lastServerMessageId <= 0) {
            throw new IllegalArgumentException("lastServerMessageId is invalid");
        }

        String listKey = ConversationWindowCacheKeys.listKey(ownerUserId);
        String metaKey = ConversationWindowCacheKeys.metaKey(ownerUserId);
        String initKey = ConversationWindowCacheKeys.initKey(ownerUserId);
        String processedKey = ConversationWindowCacheKeys.processedKey(ownerUserId, conversationId);
        String result = stringRedisTemplate.execute(
                PROJECT_WINDOW_EVENT_SCRIPT,
                List.of(listKey, metaKey, initKey, processedKey),
                conversationId,
                String.valueOf(targetId),
                lastMessage == null ? "" : lastMessage,
                lastMessageTime.toString(),
                String.valueOf(lastServerMessageId),
                incrementUnread ? "1" : "0",
                String.valueOf(ConversationWindowCacheTuning.CACHE_TTL.toSeconds()),
                String.valueOf(toScoreMillis(lastMessageTime))
        );
        if (result == null || result.isBlank()) {
            return null;
        }
        trimOverflow(listKey, metaKey);
        return readWindowValue(result);
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
        return window.getLastMessageTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    private double toScore(ConversationWindowCacheValue window) {
        if (window == null || window.getLastMessageTime() == null) {
            return 0D;
        }
        return window.getLastMessageTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    private long toScoreMillis(LocalDateTime lastMessageTime) {
        if (lastMessageTime == null) {
            return 0L;
        }
        return lastMessageTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
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
        value.setLastServerMessageId(window.getLastServerMessageId());
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
        window.setLastServerMessageId(value.getLastServerMessageId());
        window.setUnreadCount(value.getUnreadCount());
        window.setIsMuted(value.getIsMuted());
        return window;
    }

    private ConversationWindowCacheValue prepareSnapshot(ConversationWindowCacheValue value) {
        if (value == null) {
            return null;
        }
        if (value.getLastServerMessageId() != null) {
            String serverMessageId = String.valueOf(value.getLastServerMessageId());
            if (value.getUnreadBaselineServerMessageIdText() == null
                    || value.getUnreadBaselineServerMessageIdText().isBlank()) {
                value.setUnreadBaselineServerMessageIdText(serverMessageId);
            }
        }
        if (value.getUnreadCount() == null) {
            value.setUnreadCount(0);
        }
        if (value.getIsMuted() == null) {
            value.setIsMuted(0);
        }
        return value;
    }
}
