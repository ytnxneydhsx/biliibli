package com.bilibili.im.websocket.service;

public interface ImRealtimePushIdempotencyService {

    /**
     * Redis SETNX based short-lived deduplication for realtime websocket push.
     * key = im:push:{senderId}:{clientMessageId}
     * Only the first successful acquisition should trigger websocket push.
     */
    boolean tryAcquire(Long senderId, Long clientMessageId);
}
