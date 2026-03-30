package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Service
public class ConversationWindowPushServiceImpl implements ConversationWindowPushService {

    private final ImWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public ConversationWindowPushServiceImpl(ImWebSocketSessionRegistry sessionRegistry,
                                             ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void pushSingleConversationUpdated(Long ownerUserId, ConversationWindowUpdateDTO update) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (update == null) {
            return;
        }

        List<WebSocketSession> sessions = sessionRegistry.getSessions(ownerUserId);
        if (sessions.isEmpty()) {
            return;
        }

        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(ImWebSocketMessageType.CONVERSATION_UPDATED.getCode());
        outboundMessage.setCode(0);
        outboundMessage.setMessage("OK");
        outboundMessage.setData(update);

        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outboundMessage)));
            } catch (Exception ex) {
                sessionRegistry.unregister(ownerUserId, session.getId());
            }
        }
    }
}
