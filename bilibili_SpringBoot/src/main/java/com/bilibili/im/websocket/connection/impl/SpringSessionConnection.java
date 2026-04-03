package com.bilibili.im.websocket.connection.impl;

import com.bilibili.im.websocket.connection.ImSessionConnection;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class SpringSessionConnection implements ImSessionConnection {

    private final Long userId;
    private final WebSocketSession session;

    public SpringSessionConnection(Long userId, WebSocketSession session) {
        this.userId = userId;
        this.session = session;
    }

    @Override
    public String getId() {
        return session == null ? null : session.getId();
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    @Override
    public void sendText(String text) throws Exception {
        if (session == null) {
            throw new IllegalStateException("session is null");
        }
        session.sendMessage(new TextMessage(text));
    }

    @Override
    public void close(String reason) throws Exception {
        if (session == null || !session.isOpen()) {
            return;
        }
        if ("heartbeat_timeout".equals(reason)) {
            session.close(CloseStatus.SESSION_NOT_RELIABLE);
            return;
        }
        session.close();
    }
}
