package com.bilibili.im.websocket.service;

public interface ImRealtimePushIdempotencyService {

    /**
     * TODO: use Redis SETNX with a short TTL, e.g.
     * key = im:push:{senderId}:{clientMessageId}
     * Only the first successful acquisition should trigger websocket push.
     */
    boolean tryAcquire(Long senderId, Long clientMessageId);
}
