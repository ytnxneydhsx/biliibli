package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("imWebSocketProperties")
public class ImWebSocketProperties {

    @Value("${app.im.websocket.enabled:true}")
    private boolean enabled;

    @Value("${app.im.websocket.path:/ws/im}")
    private String path;

    @Value("${app.im.websocket.allowedOrigins:http://localhost:63342,http://127.0.0.1:63342,http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins;

    @Value("${app.im.websocket.heartbeatTimeoutMillis:90000}")
    private long heartbeatTimeoutMillis;

    @Value("${app.im.websocket.heartbeatCleanupIntervalMillis:30000}")
    private long heartbeatCleanupIntervalMillis;

    public boolean isEnabled() {
        return enabled;
    }

    public String getPath() {
        return path;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public long getHeartbeatTimeoutMillis() {
        return heartbeatTimeoutMillis;
    }

    public long getHeartbeatCleanupIntervalMillis() {
        return heartbeatCleanupIntervalMillis;
    }
}
