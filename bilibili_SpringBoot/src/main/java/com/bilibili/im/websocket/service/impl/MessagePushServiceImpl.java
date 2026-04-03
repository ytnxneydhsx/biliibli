package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.dto.MessagePushDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.MessagePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessagePushServiceImpl implements MessagePushService {

    private final ImConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    public MessagePushServiceImpl(ImConnectionRegistry connectionRegistry,
                                  ObjectMapper objectMapper) {
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void pushMessageReceived(Long userId, MessagePushDTO message) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (message == null) {
            return;
        }

        List<ImSessionConnection> connections = connectionRegistry.getConnections(userId);
        if (connections.isEmpty()) {
            return;
        }

        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(ImWebSocketMessageType.MESSAGE_RECEIVED.getCode());
        outboundMessage.setCode(0);
        outboundMessage.setMessage("OK");
        outboundMessage.setData(message);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(outboundMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("serialize websocket outbound message failed", ex);
        }

        for (ImSessionConnection connection : connections) {
            if (connection == null || !connection.isOpen()) {
                continue;
            }
            try {
                connection.sendText(payload);
            } catch (Exception ex) {
                connectionRegistry.unregister(userId, connection.getId());
            }
        }
    }
}
