package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationWindowPushServiceImpl implements ConversationWindowPushService {

    private final ImConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    public ConversationWindowPushServiceImpl(ImConnectionRegistry connectionRegistry,
                                             ObjectMapper objectMapper) {
        this.connectionRegistry = connectionRegistry;
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

        List<ImSessionConnection> connections = connectionRegistry.getConnections(ownerUserId);
        if (connections.isEmpty()) {
            return;
        }

        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(ImWebSocketMessageType.CONVERSATION_UPDATED.getCode());
        outboundMessage.setCode(0);
        outboundMessage.setMessage("OK");
        outboundMessage.setData(update);
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
                connectionRegistry.unregister(ownerUserId, connection.getId());
            }
        }
    }
}
