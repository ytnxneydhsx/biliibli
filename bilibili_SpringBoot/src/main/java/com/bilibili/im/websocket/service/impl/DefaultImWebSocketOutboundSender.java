package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.ImWebSocketOutboundSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultImWebSocketOutboundSender implements ImWebSocketOutboundSender {

    private final ImConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;
    private final ImWebSocketMetricsRecorder metricsRecorder;

    public DefaultImWebSocketOutboundSender(ImConnectionRegistry connectionRegistry,
                                            ObjectMapper objectMapper,
                                            ImWebSocketMetricsRecorder metricsRecorder) {
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public void sendToUser(Long userId, ImWebSocketMessageType messageType, Object data) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (messageType == null) {
            throw new IllegalArgumentException("messageType is invalid");
        }
        if (data == null) {
            return;
        }

        List<ImSessionConnection> connections = connectionRegistry.getConnections(userId);
        if (connections.isEmpty()) {
            return;
        }

        String payload = serialize(messageType, data);
        for (ImSessionConnection connection : connections) {
            send(userId, connection, messageType, payload);
        }
    }

    private String serialize(ImWebSocketMessageType messageType, Object data) {
        String messageTypeCode = messageType.getCode();
        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(messageTypeCode);
        outboundMessage.setCode(0);
        outboundMessage.setMessage("OK");
        outboundMessage.setData(data);

        long serializeStartNanos = System.nanoTime();
        try {
            String payload = objectMapper.writeValueAsString(outboundMessage);
            metricsRecorder.recordPushSerialize(messageTypeCode, "success", System.nanoTime() - serializeStartNanos);
            return payload;
        } catch (Exception ex) {
            metricsRecorder.recordPushSerialize(messageTypeCode, "failure", System.nanoTime() - serializeStartNanos);
            throw new IllegalStateException("serialize websocket outbound message failed", ex);
        }
    }

    private void send(Long userId,
                      ImSessionConnection connection,
                      ImWebSocketMessageType messageType,
                      String payload) {
        if (connection == null || !connection.isOpen()) {
            return;
        }

        String messageTypeCode = messageType.getCode();
        long sendStartNanos = System.nanoTime();
        try {
            connection.sendText(payload);
            metricsRecorder.recordPushSend(messageTypeCode, "success", System.nanoTime() - sendStartNanos);
        } catch (Exception ex) {
            metricsRecorder.recordPushSend(messageTypeCode, "failure", System.nanoTime() - sendStartNanos);
            connectionRegistry.unregister(userId, connection.getId());
        }
    }
}
