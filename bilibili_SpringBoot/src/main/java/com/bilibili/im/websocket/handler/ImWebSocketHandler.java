package com.bilibili.im.websocket.handler;

import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.model.dto.ImWebSocketInboundMessageDTO;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.service.ImRealtimePushIdempotencyService;
import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import com.bilibili.im.message.model.command.SendMessageCommand;
import com.bilibili.im.message.model.vo.SendMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private final ImWebSocketSessionRegistry sessionRegistry;
    private final ImApplicationService imApplicationService;
    private final ImRealtimePushIdempotencyService realtimePushIdempotencyService;
    private final ObjectMapper objectMapper;

    public ImWebSocketHandler(ImWebSocketSessionRegistry sessionRegistry,
                              ImApplicationService imApplicationService,
                              ImRealtimePushIdempotencyService realtimePushIdempotencyService,
                              ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.imApplicationService = imApplicationService;
        this.realtimePushIdempotencyService = realtimePushIdempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("websocket userId is invalid");
        }
        sessionRegistry.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = resolveUserId(session);
        if (userId == null || userId <= 0) {
            return;
        }
        String clientIp = resolveClientIp(session);

        sessionRegistry.touch(userId, session.getId());
        if (message == null || message.getPayload() == null) {
            return;
        }

        String payload = message.getPayload().trim();
        if (payload.isEmpty()) {
            return;
        }

        ImWebSocketInboundMessageDTO inboundMessage;
        try {
            inboundMessage = objectMapper.readValue(payload, ImWebSocketInboundMessageDTO.class);
        } catch (Exception ex) {
            sendError(session, userId, "websocket message payload is invalid");
            return;
        }

        if (inboundMessage == null || inboundMessage.getType() == null || inboundMessage.getType().isBlank()) {
            sendError(session, userId, "websocket message type is invalid");
            return;
        }

        if (ImWebSocketMessageType.matches(inboundMessage.getType(), ImWebSocketMessageType.HEARTBEAT)) {
            sendSimpleMessage(session, userId, ImWebSocketMessageType.HEARTBEAT_ACK, "OK");
            return;
        }

        if (ImWebSocketMessageType.matches(inboundMessage.getType(), ImWebSocketMessageType.SEND_MESSAGE)) {
            try {
                boolean acquired = realtimePushIdempotencyService.tryAcquire(
                        userId,
                        inboundMessage.getClientMessageId()
                );
                if (!acquired) {
                    sendError(session, userId, "websocket message is duplicated");
                    return;
                }

                if (clientIp != null && !clientIp.isBlank()) {
                    session.getAttributes().put(ImWebSocketAttributes.CLIENT_IP, clientIp);
                }

                SendMessageVO sendMessageVO = imApplicationService.acceptMessage(
                        userId,
                        clientIp,
                        toSendMessageCommand(inboundMessage)
                );
                ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
                outboundMessage.setType(ImWebSocketMessageType.SEND_MESSAGE_ACCEPTED.getCode());
                outboundMessage.setCode(0);
                outboundMessage.setMessage("OK");
                outboundMessage.setData(sendMessageVO);
                sendJsonMessage(session, outboundMessage, userId);
            } catch (Exception ex) {
                sendError(session, userId, ex.getMessage());
            }
            return;
        }

        sendError(session, userId, "websocket message type is unsupported");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUserId(session);
        if (userId != null && userId > 0) {
            sessionRegistry.unregister(userId, session.getId());
        }
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

    private SendMessageCommand toSendMessageCommand(ImWebSocketInboundMessageDTO inboundMessage) {
        SendMessageCommand command = new SendMessageCommand();
        command.setReceiverId(inboundMessage.getReceiverId());
        command.setClientMessageId(inboundMessage.getClientMessageId());
        command.setMessageType(inboundMessage.getMessageType());
        command.setContent(inboundMessage.getContent());
        return command;
    }

    private void sendError(WebSocketSession session, Long userId, String message) {
        sendSimpleMessage(session, userId, ImWebSocketMessageType.ERROR,
                message == null || message.isBlank() ? "websocket message handling failed" : message, 1, null);
    }

    private void sendSimpleMessage(WebSocketSession session,
                                   Long userId,
                                   ImWebSocketMessageType type,
                                   String message) {
        sendSimpleMessage(session, userId, type, message, 0, null);
    }

    private void sendSimpleMessage(WebSocketSession session,
                                   Long userId,
                                   ImWebSocketMessageType type,
                                   String message,
                                   Integer code,
                                   Object data) {
        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(type.getCode());
        outboundMessage.setCode(code);
        outboundMessage.setMessage(message);
        outboundMessage.setData(data);
        sendJsonMessage(session, outboundMessage, userId);
    }

    private void sendJsonMessage(WebSocketSession session, Object payload, Long userId) {
        if (session == null || !session.isOpen()) {
            sessionRegistry.unregister(userId, session == null ? null : session.getId());
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ex) {
            sessionRegistry.unregister(userId, session.getId());
            throw new IllegalStateException("send websocket json message failed", ex);
        }
    }
}
