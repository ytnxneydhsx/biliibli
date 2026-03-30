package com.bilibili.im.websocket.service;

import com.bilibili.im.websocket.model.dto.MessagePushDTO;

public interface MessagePushService {

    void pushMessageReceived(Long userId, MessagePushDTO message);
}
