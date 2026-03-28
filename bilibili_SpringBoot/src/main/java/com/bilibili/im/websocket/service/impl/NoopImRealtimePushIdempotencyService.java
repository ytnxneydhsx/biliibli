package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.service.ImRealtimePushIdempotencyService;
import org.springframework.stereotype.Service;

@Service
public class NoopImRealtimePushIdempotencyService implements ImRealtimePushIdempotencyService {

    @Override
    public boolean tryAcquire(Long senderId, Long clientMessageId) {
        // TODO: replace with Redis SETNX based deduplication before realtime push.
        return true;
    }
}
