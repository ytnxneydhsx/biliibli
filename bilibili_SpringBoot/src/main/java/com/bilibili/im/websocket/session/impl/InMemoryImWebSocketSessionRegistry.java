package com.bilibili.im.websocket.session.impl;

import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryImWebSocketSessionRegistry implements ImWebSocketSessionRegistry {

    private final Map<Long, Map<String, WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void register(Long userId, WebSocketSession session) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (session == null || session.getId() == null || session.getId().isBlank()) {
            throw new IllegalArgumentException("session is invalid");
        }

        sessionsByUser.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    @Override
    public void unregister(Long userId, String sessionId) {
        if (userId == null || userId <= 0 || sessionId == null || sessionId.isBlank()) {
            return;
        }

        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    @Override
    public List<WebSocketSession> getSessions(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }

        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(sessions.values());
    }

    @Override
    public boolean isOnline(Long userId) {
        return !getSessions(userId).isEmpty();
    }
}
