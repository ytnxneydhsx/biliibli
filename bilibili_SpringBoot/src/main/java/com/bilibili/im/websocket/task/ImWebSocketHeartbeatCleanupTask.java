package com.bilibili.im.websocket.task;

import com.bilibili.config.properties.ImWebSocketProperties;
import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.im.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImWebSocketHeartbeatCleanupTask {

    private final ImWebSocketProperties properties;
    private final ImWebSocketSessionRegistry sessionRegistry;

    public ImWebSocketHeartbeatCleanupTask(ImWebSocketProperties properties,
                                           ImWebSocketSessionRegistry sessionRegistry) {
        this.properties = properties;
        this.sessionRegistry = sessionRegistry;
    }

    @Scheduled(fixedDelayString = "#{@imWebSocketProperties.getHeartbeatCleanupIntervalMillis()}")
    public void cleanupExpiredSessions() {
        long expireBeforeEpochMillis = System.currentTimeMillis() - properties.getHeartbeatTimeoutMillis();
        List<WebSocketSession> expiredSessions = sessionRegistry.removeExpiredSessions(expireBeforeEpochMillis);
        for (WebSocketSession expiredSession : expiredSessions) {
            if (expiredSession == null || !expiredSession.isOpen()) {
                continue;
            }
            try {
                expiredSession.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (Exception ignored) {
                // Session has already been removed from the registry.
            }
        }
    }
}
