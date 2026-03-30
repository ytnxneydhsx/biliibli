package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.dto.MessagePushDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.MessagePushService;
import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Service
public class MessagePushServiceImpl implements MessagePushService {

    private final ImWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public MessagePushServiceImpl(ImWebSocketSessionRegistry sessionRegistry,
                                  ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
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

        List<WebSocketSession> sessions = sessionRegistry.getSessions(userId);
        if (sessions.isEmpty()) {
            return;
        }

        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(ImWebSocketMessageType.MESSAGE_RECEIVED.getCode());
        outboundMessage.setCode(0);
        outboundMessage.setMessage("OK");
        outboundMessage.setData(message);

        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outboundMessage)));
            } catch (Exception ex) {
                sessionRegistry.unregister(userId, session.getId());
            }
        }
    }
}
