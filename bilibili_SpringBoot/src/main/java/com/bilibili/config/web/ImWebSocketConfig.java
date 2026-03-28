package com.bilibili.config.web;

import com.bilibili.config.properties.ImWebSocketProperties;
import com.bilibili.im.websocket.handler.ImWebSocketHandler;
import com.bilibili.im.websocket.interceptor.ImWebSocketHandshakeInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(prefix = "app.im.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImWebSocketConfig implements WebSocketConfigurer {

    private final ImWebSocketProperties properties;
    private final ImWebSocketHandler imWebSocketHandler;
    private final ImWebSocketHandshakeInterceptor imWebSocketHandshakeInterceptor;

    public ImWebSocketConfig(ImWebSocketProperties properties,
                             ImWebSocketHandler imWebSocketHandler,
                             ImWebSocketHandshakeInterceptor imWebSocketHandshakeInterceptor) {
        this.properties = properties;
        this.imWebSocketHandler = imWebSocketHandler;
        this.imWebSocketHandshakeInterceptor = imWebSocketHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(imWebSocketHandler, properties.getPath())
                .addInterceptors(imWebSocketHandshakeInterceptor)
                .setAllowedOrigins(resolveAllowedOrigins(properties.getAllowedOrigins()));
    }

    private String[] resolveAllowedOrigins(String allowedOrigins) {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toArray(String[]::new);
    }
}
