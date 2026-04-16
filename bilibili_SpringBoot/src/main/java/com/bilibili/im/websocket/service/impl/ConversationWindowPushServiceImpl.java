package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.dto.GroupConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationWindowPushServiceImpl implements ConversationWindowPushService {

    private final ImConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;
    private final ImWebSocketMetricsRecorder metricsRecorder;

    public ConversationWindowPushServiceImpl(ImConnectionRegistry connectionRegistry,
                                             ObjectMapper objectMapper,
                                             ImWebSocketMetricsRecorder metricsRecorder) {
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public void pushSingleConversationUpdated(Long ownerUserId, ConversationWindowUpdateDTO update) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (update == null) {
            return;
        }
        pushOutboundMessage(ownerUserId, ImWebSocketMessageType.CONVERSATION_UPDATED, update);
    }

    @Override
    public void pushGroupConversationUpdated(Long ownerUserId, GroupConversationWindowUpdateDTO update) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (update == null) {
            return;
        }
        pushOutboundMessage(ownerUserId, ImWebSocketMessageType.GROUP_CONVERSATION_UPDATED, update);
    }

    private void pushOutboundMessage(Long ownerUserId, ImWebSocketMessageType messageType, Object data) {
        List<ImSessionConnection> connections = connectionRegistry.getConnections(ownerUserId);
        if (connections.isEmpty()) {
            return;
        }

        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(messageType.getCode());
        outboundMessage.setCode(0);
        outboundMessage.setMessage("OK");
        outboundMessage.setData(data);
        String payload;
        long serializeStartNanos = System.nanoTime();
        try {
            payload = objectMapper.writeValueAsString(outboundMessage);
            metricsRecorder.recordPushSerialize(messageType.getCode(), "success", System.nanoTime() - serializeStartNanos);
        } catch (Exception ex) {
            metricsRecorder.recordPushSerialize(messageType.getCode(), "failure", System.nanoTime() - serializeStartNanos);
            throw new IllegalStateException("serialize websocket outbound message failed", ex);
        }

        for (ImSessionConnection connection : connections) {
            if (connection == null || !connection.isOpen()) {
                continue;
            }
            long sendStartNanos = System.nanoTime();
            try {
                connection.sendText(payload);
                metricsRecorder.recordPushSend(messageType.getCode(), "success", System.nanoTime() - sendStartNanos);
            } catch (Exception ex) {
                metricsRecorder.recordPushSend(messageType.getCode(), "failure", System.nanoTime() - sendStartNanos);
                connectionRegistry.unregister(ownerUserId, connection.getId());
            }
        }
    }
}
