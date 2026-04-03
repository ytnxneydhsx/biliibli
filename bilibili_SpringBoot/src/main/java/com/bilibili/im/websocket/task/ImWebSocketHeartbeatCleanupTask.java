package com.bilibili.im.websocket.task;

import com.bilibili.config.properties.ImWebSocketProperties;
import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.connection.ImSessionConnection;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.im.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImWebSocketHeartbeatCleanupTask {

    private final ImWebSocketProperties properties;
    private final ImConnectionRegistry connectionRegistry;
    private final ImWebSocketMetricsRecorder metricsRecorder;

    public ImWebSocketHeartbeatCleanupTask(ImWebSocketProperties properties,
                                           ImConnectionRegistry connectionRegistry,
                                           ImWebSocketMetricsRecorder metricsRecorder) {
        this.properties = properties;
        this.connectionRegistry = connectionRegistry;
        this.metricsRecorder = metricsRecorder;
    }

    @Scheduled(fixedDelayString = "#{@imWebSocketProperties.getHeartbeatCleanupIntervalMillis()}")
    public void cleanupExpiredSessions() {
        long expireBeforeEpochMillis = System.currentTimeMillis() - properties.getHeartbeatTimeoutMillis();
        List<ImSessionConnection> expiredConnections = connectionRegistry.removeExpiredConnections(expireBeforeEpochMillis);
        metricsRecorder.recordExpiredSessionCleanup(expiredConnections.size());
        for (ImSessionConnection expiredConnection : expiredConnections) {
            if (expiredConnection == null || !expiredConnection.isOpen()) {
                continue;
            }
            try {
                expiredConnection.close("heartbeat_timeout");
            } catch (Exception ignored) {
                // Connection has already been removed from the registry.
            }
        }
    }
}
