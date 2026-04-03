package com.bilibili.im.websocket.connection.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryImConnectionRegistry implements ImConnectionRegistry {

    private final Map<Long, Map<String, ConnectionRecord>> connectionsByUser = new ConcurrentHashMap<>();

    @Override
    public void register(ImSessionConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is invalid");
        }
        Long userId = connection.getUserId();
        String connectionId = connection.getId();
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("connectionId is invalid");
        }

        connectionsByUser.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(connectionId, ConnectionRecord.fromConnection(connection));
    }

    @Override
    public void unregister(Long userId, String connectionId) {
        if (userId == null || userId <= 0 || connectionId == null || connectionId.isBlank()) {
            return;
        }

        Map<String, ConnectionRecord> connections = connectionsByUser.get(userId);
        if (connections == null) {
            return;
        }

        connections.remove(connectionId);
        if (connections.isEmpty()) {
            connectionsByUser.remove(userId);
        }
    }

    @Override
    public void touch(Long userId, String connectionId) {
        if (userId == null || userId <= 0 || connectionId == null || connectionId.isBlank()) {
            return;
        }

        Map<String, ConnectionRecord> connections = connectionsByUser.get(userId);
        if (connections == null) {
            return;
        }

        ConnectionRecord connectionRecord = connections.get(connectionId);
        if (connectionRecord == null) {
            return;
        }
        connectionRecord.touch();
    }

    @Override
    public List<ImSessionConnection> getConnections(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }

        Map<String, ConnectionRecord> connections = connectionsByUser.get(userId);
        if (connections == null || connections.isEmpty()) {
            return Collections.emptyList();
        }

        List<ImSessionConnection> result = new ArrayList<>();
        List<String> closedConnectionIds = new ArrayList<>();
        for (Map.Entry<String, ConnectionRecord> entry : connections.entrySet()) {
            ConnectionRecord connectionRecord = entry.getValue();
            if (connectionRecord == null || !connectionRecord.isOpen()) {
                closedConnectionIds.add(entry.getKey());
                continue;
            }
            result.add(connectionRecord.toConnection());
        }
        removeClosedConnections(userId, connections, closedConnectionIds);
        return result;
    }

    @Override
    public boolean isOnline(Long userId) {
        return !getConnections(userId).isEmpty();
    }

    @Override
    public List<ImSessionConnection> removeExpiredConnections(long expireBeforeEpochMillis) {
        if (expireBeforeEpochMillis <= 0) {
            return Collections.emptyList();
        }

        List<ImSessionConnection> expiredConnections = new ArrayList<>();
        List<Long> emptyUserIds = new ArrayList<>();

        for (Map.Entry<Long, Map<String, ConnectionRecord>> userEntry : connectionsByUser.entrySet()) {
            Long userId = userEntry.getKey();
            Map<String, ConnectionRecord> connections = userEntry.getValue();
            if (connections == null || connections.isEmpty()) {
                emptyUserIds.add(userId);
                continue;
            }

            List<String> expiredConnectionIds = new ArrayList<>();
            for (Map.Entry<String, ConnectionRecord> connectionEntry : connections.entrySet()) {
                ConnectionRecord connectionRecord = connectionEntry.getValue();
                if (connectionRecord == null || connectionRecord.shouldRemove(expireBeforeEpochMillis)) {
                    expiredConnectionIds.add(connectionEntry.getKey());
                    if (connectionRecord != null && connectionRecord.toConnection() != null) {
                        expiredConnections.add(connectionRecord.toConnection());
                    }
                }
            }

            for (String expiredConnectionId : expiredConnectionIds) {
                connections.remove(expiredConnectionId);
            }
            if (connections.isEmpty()) {
                emptyUserIds.add(userId);
            }
        }

        for (Long emptyUserId : emptyUserIds) {
            connectionsByUser.remove(emptyUserId);
        }
        return expiredConnections;
    }

    @Override
    public int countOpenConnections() {
        int count = 0;
        for (Map<String, ConnectionRecord> connections : connectionsByUser.values()) {
            if (connections == null || connections.isEmpty()) {
                continue;
            }
            for (ConnectionRecord connectionRecord : connections.values()) {
                if (connectionRecord != null && connectionRecord.isOpen()) {
                    count += 1;
                }
            }
        }
        return count;
    }

    @Override
    public int countOnlineUsers() {
        int count = 0;
        for (Map<String, ConnectionRecord> connections : connectionsByUser.values()) {
            if (connections == null || connections.isEmpty()) {
                continue;
            }
            boolean online = false;
            for (ConnectionRecord connectionRecord : connections.values()) {
                if (connectionRecord != null && connectionRecord.isOpen()) {
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

    private void removeClosedConnections(Long userId,
                                         Map<String, ConnectionRecord> connections,
                                         List<String> closedConnectionIds) {
        if (closedConnectionIds.isEmpty()) {
            return;
        }

        for (String closedConnectionId : closedConnectionIds) {
            connections.remove(closedConnectionId);
        }
        if (connections.isEmpty()) {
            connectionsByUser.remove(userId);
        }
    }

    private static final class ConnectionRecord {

        private final ImSessionConnection connection;
        private volatile long lastActiveTimeMillis;

        private static ConnectionRecord fromConnection(ImSessionConnection connection) {
            return new ConnectionRecord(connection, System.currentTimeMillis());
        }

        private ConnectionRecord(ImSessionConnection connection, long lastActiveTimeMillis) {
            this.connection = connection;
            this.lastActiveTimeMillis = lastActiveTimeMillis;
        }

        private ImSessionConnection toConnection() {
            return connection;
        }

        private boolean isOpen() {
            return connection != null && connection.isOpen();
        }

        private boolean shouldRemove(long expireBeforeEpochMillis) {
            return !isOpen() || lastActiveTimeMillis < expireBeforeEpochMillis;
        }

        private void touch() {
            this.lastActiveTimeMillis = System.currentTimeMillis();
        }
    }
}
