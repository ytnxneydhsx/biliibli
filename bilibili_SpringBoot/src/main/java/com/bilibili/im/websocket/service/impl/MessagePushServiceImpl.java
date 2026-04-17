package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.model.dto.MessagePushDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.ImWebSocketOutboundSender;
import com.bilibili.im.websocket.service.MessagePushService;
import org.springframework.stereotype.Service;

@Service
public class MessagePushServiceImpl implements MessagePushService {

    private final ImWebSocketOutboundSender outboundSender;

    public MessagePushServiceImpl(ImWebSocketOutboundSender outboundSender) {
        this.outboundSender = outboundSender;
    }

    @Override
    public void pushMessageReceived(Long userId, MessagePushDTO message) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (message == null) {
            return;
        }
        outboundSender.sendToUser(userId, ImWebSocketMessageType.MESSAGE_RECEIVED, message);
    }
}
