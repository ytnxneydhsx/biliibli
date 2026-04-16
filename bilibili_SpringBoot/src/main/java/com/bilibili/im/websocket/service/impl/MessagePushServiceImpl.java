package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
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
    private final ImWebSocketMetricsRecorder metricsRecorder;

    public MessagePushServiceImpl(ImConnectionRegistry connectionRegistry,
                                  ObjectMapper objectMapper,
                                  ImWebSocketMetricsRecorder metricsRecorder) {
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
        this.metricsRecorder = metricsRecorder;
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
        long serializeStartNanos = System.nanoTime();
        try {
            payload = objectMapper.writeValueAsString(outboundMessage);
            metricsRecorder.recordPushSerialize(ImWebSocketMessageType.MESSAGE_RECEIVED.getCode(),
                    "success",
                    System.nanoTime() - serializeStartNanos);
        } catch (Exception ex) {
            metricsRecorder.recordPushSerialize(ImWebSocketMessageType.MESSAGE_RECEIVED.getCode(),
                    "failure",
                    System.nanoTime() - serializeStartNanos);
            throw new IllegalStateException("serialize websocket outbound message failed", ex);
        }

        for (ImSessionConnection connection : connections) {
            if (connection == null || !connection.isOpen()) {
                continue;
            }
            long sendStartNanos = System.nanoTime();
            try {
                connection.sendText(payload);
                metricsRecorder.recordPushSend(ImWebSocketMessageType.MESSAGE_RECEIVED.getCode(),
                        "success",
                        System.nanoTime() - sendStartNanos);
            } catch (Exception ex) {
                metricsRecorder.recordPushSend(ImWebSocketMessageType.MESSAGE_RECEIVED.getCode(),
                        "failure",
                        System.nanoTime() - sendStartNanos);
                connectionRegistry.unregister(userId, connection.getId());
            }
        }
    }
}
