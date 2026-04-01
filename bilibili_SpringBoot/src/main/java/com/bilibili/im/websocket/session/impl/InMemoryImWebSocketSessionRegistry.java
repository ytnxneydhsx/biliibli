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

    private final Map<Long, Map<String, SessionRecord>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void register(Long userId, WebSocketSession session) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (session == null || session.getId() == null || session.getId().isBlank()) {
            throw new IllegalArgumentException("session is invalid");
        }

        sessionsByUser.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), new SessionRecord(session, System.currentTimeMillis()));
    }

    @Override
    public void unregister(Long userId, String sessionId) {
        if (userId == null || userId <= 0 || sessionId == null || sessionId.isBlank()) {
            return;
        }

        Map<String, SessionRecord> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    @Override
    public void touch(Long userId, String sessionId) {
        if (userId == null || userId <= 0 || sessionId == null || sessionId.isBlank()) {
            return;
        }

        Map<String, SessionRecord> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }

        SessionRecord sessionRecord = sessions.get(sessionId);
        if (sessionRecord == null) {
            return;
        }
        sessionRecord.touch();
    }

    @Override
    public List<WebSocketSession> getSessions(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }

        Map<String, SessionRecord> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        List<WebSocketSession> result = new ArrayList<>();
        List<String> closedSessionIds = new ArrayList<>();
        for (Map.Entry<String, SessionRecord> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue().getSession();
            if (session == null || !session.isOpen()) {
                closedSessionIds.add(entry.getKey());
                continue;
            }
            result.add(session);
        }
        removeClosedSessions(userId, sessions, closedSessionIds);
        return result;
    }

    @Override
    public boolean isOnline(Long userId) {
        return !getSessions(userId).isEmpty();
    }

    @Override
    public List<WebSocketSession> removeExpiredSessions(long expireBeforeEpochMillis) {
        if (expireBeforeEpochMillis <= 0) {
            return Collections.emptyList();
        }

        List<WebSocketSession> expiredSessions = new ArrayList<>();
        List<Long> emptyUserIds = new ArrayList<>();

        for (Map.Entry<Long, Map<String, SessionRecord>> userEntry : sessionsByUser.entrySet()) {
            Long userId = userEntry.getKey();
            Map<String, SessionRecord> sessions = userEntry.getValue();
            if (sessions == null || sessions.isEmpty()) {
                emptyUserIds.add(userId);
                continue;
            }

            List<String> expiredSessionIds = new ArrayList<>();
            for (Map.Entry<String, SessionRecord> sessionEntry : sessions.entrySet()) {
                SessionRecord sessionRecord = sessionEntry.getValue();
                if (sessionRecord == null) {
                    expiredSessionIds.add(sessionEntry.getKey());
                    continue;
                }
                WebSocketSession session = sessionRecord.getSession();
                if (session == null || !session.isOpen() || sessionRecord.getLastActiveTimeMillis() < expireBeforeEpochMillis) {
                    expiredSessionIds.add(sessionEntry.getKey());
                    if (session != null) {
                        expiredSessions.add(session);
                    }
                }
            }

            for (String expiredSessionId : expiredSessionIds) {
                sessions.remove(expiredSessionId);
            }
            if (sessions.isEmpty()) {
                emptyUserIds.add(userId);
            }
        }

        for (Long emptyUserId : emptyUserIds) {
            sessionsByUser.remove(emptyUserId);
        }
        return expiredSessions;
    }

    @Override
    public int countOpenSessions() {
        int count = 0;
        for (Map<String, SessionRecord> sessions : sessionsByUser.values()) {
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }
            for (SessionRecord sessionRecord : sessions.values()) {
                if (sessionRecord == null) {
                    continue;
                }
                WebSocketSession session = sessionRecord.getSession();
                if (session != null && session.isOpen()) {
                    count += 1;
                }
            }
        }
        return count;
    }

    @Override
    public int countOnlineUsers() {
        int count = 0;
        for (Map<String, SessionRecord> sessions : sessionsByUser.values()) {
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }
            boolean online = false;
            for (SessionRecord sessionRecord : sessions.values()) {
                if (sessionRecord == null) {
                    continue;
                }
                WebSocketSession session = sessionRecord.getSession();
                if (session != null && session.isOpen()) {
                    online = true;
                    break;
                }
            }
            if (online) {
                count += 1;
            }
        }
        return count;
    }

    private void removeClosedSessions(Long userId,
                                      Map<String, SessionRecord> sessions,
                                      List<String> closedSessionIds) {
        if (closedSessionIds.isEmpty()) {
            return;
        }

        for (String closedSessionId : closedSessionIds) {
            sessions.remove(closedSessionId);
        }
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    private static final class SessionRecord {

        private final WebSocketSession session;
        private volatile long lastActiveTimeMillis;

        private SessionRecord(WebSocketSession session, long lastActiveTimeMillis) {
            this.session = session;
            this.lastActiveTimeMillis = lastActiveTimeMillis;
        }

        private WebSocketSession getSession() {
            return session;
        }

        private long getLastActiveTimeMillis() {
            return lastActiveTimeMillis;
        }

        private void touch() {
            this.lastActiveTimeMillis = System.currentTimeMillis();
        }
    }
}
