package com.bilibili.im.websocket.handler;

import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private final ImWebSocketSessionRegistry sessionRegistry;

    public ImWebSocketHandler(ImWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
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
        // Client message handling will be added later.
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
}
