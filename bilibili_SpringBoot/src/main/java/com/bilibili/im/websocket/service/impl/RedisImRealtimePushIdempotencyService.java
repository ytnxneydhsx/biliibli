package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.service.ImRealtimePushIdempotencyService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisImRealtimePushIdempotencyService implements ImRealtimePushIdempotencyService {

    private static final String KEY_PREFIX = "im:push:";
    private static final String ACQUIRED_VALUE = "1";
    private static final Duration TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisImRealtimePushIdempotencyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryAcquire(Long senderId, Long clientMessageId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (clientMessageId == null || clientMessageId <= 0) {
            throw new IllegalArgumentException("clientMessageId is invalid");
        }

        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                buildKey(senderId, clientMessageId),
                ACQUIRED_VALUE,
                TTL
        );
        return Boolean.TRUE.equals(acquired);
    }

    private String buildKey(Long senderId, Long clientMessageId) {
        return KEY_PREFIX + senderId + ":" + clientMessageId;
    }
}
