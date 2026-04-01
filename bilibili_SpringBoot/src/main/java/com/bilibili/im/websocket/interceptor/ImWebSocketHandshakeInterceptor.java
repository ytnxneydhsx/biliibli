package com.bilibili.im.websocket.interceptor;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.security.resolver.ClientIpResolver;
import com.bilibili.security.resolver.AuthenticatedUserResolver;
import com.bilibili.security.resolver.TokenResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class ImWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenResolver tokenResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ClientIpResolver clientIpResolver;
    private final ImWebSocketMetricsRecorder metricsRecorder;

    public ImWebSocketHandshakeInterceptor(TokenResolver tokenResolver,
                                           AuthenticatedUserResolver authenticatedUserResolver,
                                           ClientIpResolver clientIpResolver,
                                           ImWebSocketMetricsRecorder metricsRecorder) {
        this.tokenResolver = tokenResolver;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.clientIpResolver = clientIpResolver;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        long startNanos = System.nanoTime();
        metricsRecorder.recordHandshakeAttempt();

        String token = tokenResolver.resolve(request);
        if (token == null || token.isBlank()) {
            recordHandshakeFailure(startNanos, "missing_token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            AuthenticatedUser authenticatedUser = authenticatedUserResolver.resolve(token);
            attributes.put(ImWebSocketAttributes.USER_ID, authenticatedUser.getUid());
            attributes.put(ImWebSocketAttributes.CLIENT_IP, clientIpResolver.resolve(request));
            metricsRecorder.recordHandshakeSuccess(System.nanoTime() - startNanos);
            return true;
        } catch (Exception ex) {
            recordHandshakeFailure(startNanos, "invalid_token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private void recordHandshakeFailure(long startNanos, String reason) {
        metricsRecorder.recordHandshakeFailure(reason, System.nanoTime() - startNanos);
    }
}
