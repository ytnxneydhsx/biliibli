package com.bilibili.im.mq.consumer;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ImConsumerDedupeService {

    private static final Duration DEFAULT_TTL = Duration.ofDays(1);
    private static final String KEY_PREFIX = "im:consumer:dedupe:";

    private final StringRedisTemplate stringRedisTemplate;

    public ImConsumerDedupeService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryAcquire(String consumerName, Long serverMessageId) {
        validate(consumerName, serverMessageId);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key(consumerName, serverMessageId), "1", DEFAULT_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    public void release(String consumerName, Long serverMessageId) {
        validate(consumerName, serverMessageId);
        stringRedisTemplate.delete(key(consumerName, serverMessageId));
    }

    private void validate(String consumerName, Long serverMessageId) {
        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException("consumerName is invalid");
        }
        if (serverMessageId == null || serverMessageId <= 0) {
            throw new IllegalArgumentException("serverMessageId is invalid");
        }
    }

    private String key(String consumerName, Long serverMessageId) {
        return KEY_PREFIX + consumerName + ":" + serverMessageId;
    }
}
