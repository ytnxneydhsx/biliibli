package com.bilibili.im.websocket.session;

import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface ImWebSocketSessionRegistry {

    void register(Long userId, WebSocketSession session);

    void unregister(Long userId, String sessionId);

    void touch(Long userId, String sessionId);

    List<WebSocketSession> getSessions(Long userId);

    boolean isOnline(Long userId);

    List<WebSocketSession> removeExpiredSessions(long expireBeforeEpochMillis);
}
