package com.bilibili.im.websocket.handler;

import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.impl.SpringSessionConnection;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.im.websocket.model.dto.ImWebSocketInboundMessageDTO;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.protocol.ImProtocolCodec;
import com.bilibili.im.websocket.protocol.ImProtocolDispatcher;
import com.bilibili.im.websocket.protocol.ImProtocolResponseFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private final ImConnectionRegistry connectionRegistry;
    private final ImProtocolCodec protocolCodec;
    private final ImProtocolDispatcher protocolDispatcher;
    private final ImProtocolResponseFactory responseFactory;
    private final ImWebSocketMetricsRecorder metricsRecorder;

    public ImWebSocketHandler(ImConnectionRegistry connectionRegistry,
                              ImProtocolCodec protocolCodec,
                              ImProtocolDispatcher protocolDispatcher,
                              ImProtocolResponseFactory responseFactory,
                              ImWebSocketMetricsRecorder metricsRecorder) {
        this.connectionRegistry = connectionRegistry;
        this.protocolCodec = protocolCodec;
        this.protocolDispatcher = protocolDispatcher;
        this.responseFactory = responseFactory;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("websocket userId is invalid");
        }
        connectionRegistry.register(new SpringSessionConnection(userId, session));
        metricsRecorder.recordConnectionOpened();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = resolveUserId(session);
        if (userId == null || userId <= 0) {
            return;
        }
        String clientIp = resolveClientIp(session);

        connectionRegistry.touch(userId, session.getId());
        if (message == null || message.getPayload() == null) {
            return;
        }

        String payload = message.getPayload().trim();
        if (payload.isEmpty()) {
            return;
        }

        ImWebSocketInboundMessageDTO inboundMessage;
        try {
            inboundMessage = protocolCodec.decodeInbound(payload);
        } catch (Exception ex) {
            metricsRecorder.recordInboundPayloadInvalid();
            sendOutboundMessage(session, userId, responseFactory.error("websocket message payload is invalid"));
            return;
        }

        if (inboundMessage == null || inboundMessage.getType() == null || inboundMessage.getType().isBlank()) {
            metricsRecorder.recordInboundTypeInvalid();
            sendOutboundMessage(session, userId, responseFactory.error("websocket message type is invalid"));
            return;
        }

        ImWebSocketOutboundMessageDTO outboundMessage = protocolDispatcher.dispatch(userId, clientIp, inboundMessage);
        sendOutboundMessage(session, userId, outboundMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUserId(session);
        if (userId != null && userId > 0) {
            connectionRegistry.unregister(userId, session.getId());
        }
        metricsRecorder.recordConnectionClosed(status == null ? "unknown" : "code_" + status.getCode());
    }

    private Long resolveUserId(WebSocketSession session) {
        if (session == null) {
            return null;
        }
        Object userId = session.getAttributes().get(ImWebSocketAttributes.USER_ID);
        if (userId instanceof Long) {
            return (Long) userId;
        }
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    private String resolveClientIp(WebSocketSession session) {
        if (session == null) {
            return null;
        }
        Object clientIp = session.getAttributes().get(ImWebSocketAttributes.CLIENT_IP);
        if (clientIp instanceof String value) {
            return value;
        }
        return null;
    }

    private void sendOutboundMessage(WebSocketSession session, Long userId, ImWebSocketOutboundMessageDTO payload) {
        if (session == null || !session.isOpen()) {
            connectionRegistry.unregister(userId, session == null ? null : session.getId());
            return;
        }
        try {
            session.sendMessage(new TextMessage(protocolCodec.encodeOutbound(payload)));
        } catch (Exception ex) {
            if ("heartbeat_ack".equals(payload.getType())) {
                metricsRecorder.recordHeartbeatAckFailed();
            }
            connectionRegistry.unregister(userId, session.getId());
            throw new IllegalStateException("send websocket json message failed", ex);
        }
    }
}
