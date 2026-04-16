package com.bilibili.im.websocket.handler;

import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
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
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

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
        WebSocketSession concurrentSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLIS,
                SEND_BUFFER_SIZE_LIMIT_BYTES
        );
        ImSessionConnection connection = new SpringSessionConnection(userId, concurrentSession);
        connectionRegistry.register(connection);
        metricsRecorder.recordConnectionOpened();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        long handleStartNanos = System.nanoTime();
        String inboundType = "unknown";
        String outcome = "success";
        try {
            Long userId = resolveUserId(session);
            if (userId == null || userId <= 0) {
                outcome = "invalid_user";
                return;
            }
            String clientIp = resolveClientIp(session);

            connectionRegistry.touch(userId, session.getId());
            if (message == null || message.getPayload() == null) {
                outcome = "empty_payload";
                return;
            }

            String payload = message.getPayload().trim();
            if (payload.isEmpty()) {
                outcome = "empty_payload";
                return;
            }

            ImWebSocketInboundMessageDTO inboundMessage;
            long decodeStartNanos = System.nanoTime();
            try {
                inboundMessage = protocolCodec.decodeInbound(payload);
                metricsRecorder.recordInboundDecode("success", System.nanoTime() - decodeStartNanos);
            } catch (Exception ex) {
                metricsRecorder.recordInboundDecode("failure", System.nanoTime() - decodeStartNanos);
                metricsRecorder.recordInboundPayloadInvalid();
                outcome = "invalid_payload";
                sendOutboundMessage(session, userId, responseFactory.error("websocket message payload is invalid"));
                return;
            }

            if (inboundMessage == null || inboundMessage.getType() == null || inboundMessage.getType().isBlank()) {
                metricsRecorder.recordInboundTypeInvalid();
                outcome = "invalid_type";
                sendOutboundMessage(session, userId, responseFactory.error("websocket message type is invalid"));
                return;
            }

            inboundType = inboundMessage.getType();
            ImWebSocketOutboundMessageDTO outboundMessage = protocolDispatcher.dispatch(userId, clientIp, inboundMessage);
            sendOutboundMessage(session, userId, outboundMessage);
        } catch (RuntimeException ex) {
            outcome = "failure";
            throw ex;
        } finally {
            metricsRecorder.recordInboundHandle(inboundType, outcome, System.nanoTime() - handleStartNanos);
        }
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
        String outboundType = payload == null ? "unknown" : payload.getType();
        ImSessionConnection connection = session == null
                ? null
                : connectionRegistry.getConnection(userId, session.getId());
        if (connection == null || !connection.isOpen()) {
            connectionRegistry.unregister(userId, session == null ? null : session.getId());
            return;
        }
        String text;
        long encodeStartNanos = System.nanoTime();
        try {
            text = protocolCodec.encodeOutbound(payload);
            metricsRecorder.recordOutboundEncode(outboundType, "success", System.nanoTime() - encodeStartNanos);
        } catch (Exception ex) {
            metricsRecorder.recordOutboundEncode(outboundType, "failure", System.nanoTime() - encodeStartNanos);
            throw new IllegalStateException("encode websocket json message failed", ex);
        }

        long sendStartNanos = System.nanoTime();
        try {
            connection.sendText(text);
            metricsRecorder.recordOutboundSend(outboundType, "success", System.nanoTime() - sendStartNanos);
        } catch (Exception ex) {
            metricsRecorder.recordOutboundSend(outboundType, "failure", System.nanoTime() - sendStartNanos);
            if ("heartbeat_ack".equals(outboundType)) {
                metricsRecorder.recordHeartbeatAckFailed();
            }
            connectionRegistry.unregister(userId, connection.getId());
            throw new IllegalStateException("send websocket json message failed", ex);
        }
    }
}
