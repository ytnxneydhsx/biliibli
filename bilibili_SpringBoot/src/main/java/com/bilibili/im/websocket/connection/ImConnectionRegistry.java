package com.bilibili.im.websocket.connection;

import java.util.List;

public interface ImConnectionRegistry {

    void register(ImSessionConnection connection);

    void unregister(Long userId, String connectionId);

    void touch(Long userId, String connectionId);

    List<ImSessionConnection> getConnections(Long userId);

    boolean isOnline(Long userId);

    List<ImSessionConnection> removeExpiredConnections(long expireBeforeEpochMillis);

    int countOpenConnections();

    int countOnlineUsers();
}
