package com.bilibili.im.websocket.interceptor;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.security.resolver.ClientIpResolver;
import com.bilibili.security.resolver.AuthenticatedUserResolver;
import com.bilibili.security.resolver.TokenResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ImWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenResolver tokenResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ClientIpResolver clientIpResolver;
    private final Counter handshakeAttempts;
    private final Counter handshakeSuccesses;
    private final Counter handshakeFailures;
    private final Counter handshakeMissingTokenFailures;
    private final Counter handshakeInvalidTokenFailures;
    private final Timer handshakeSuccessTimer;
    private final Timer handshakeFailureTimer;

    public ImWebSocketHandshakeInterceptor(TokenResolver tokenResolver,
                                           AuthenticatedUserResolver authenticatedUserResolver,
                                           ClientIpResolver clientIpResolver,
                                           MeterRegistry meterRegistry) {
        this.tokenResolver = tokenResolver;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.clientIpResolver = clientIpResolver;
        this.handshakeAttempts = Counter.builder("im.ws.handshake.attempts")
                .description("Total websocket handshake attempts")
                .register(meterRegistry);
        this.handshakeSuccesses = Counter.builder("im.ws.handshake.success")
                .description("Successful websocket handshakes")
                .register(meterRegistry);
        this.handshakeFailures = Counter.builder("im.ws.handshake.failure")
                .description("Failed websocket handshakes")
                .register(meterRegistry);
        this.handshakeMissingTokenFailures = Counter.builder("im.ws.handshake.failure.reason")
                .description("Websocket handshake failures grouped by reason")
                .tag("reason", "missing_token")
                .register(meterRegistry);
        this.handshakeInvalidTokenFailures = Counter.builder("im.ws.handshake.failure.reason")
                .description("Websocket handshake failures grouped by reason")
                .tag("reason", "invalid_token")
                .register(meterRegistry);
        this.handshakeSuccessTimer = Timer.builder("im.ws.handshake.duration")
                .description("Websocket handshake duration")
                .tag("outcome", "success")
                .register(meterRegistry);
        this.handshakeFailureTimer = Timer.builder("im.ws.handshake.duration")
                .description("Websocket handshake duration")
                .tag("outcome", "failure")
                .register(meterRegistry);
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        long startNanos = System.nanoTime();
        handshakeAttempts.increment();

        String token = tokenResolver.resolve(request);
        if (token == null || token.isBlank()) {
            recordHandshakeFailure(startNanos, handshakeMissingTokenFailures);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            AuthenticatedUser authenticatedUser = authenticatedUserResolver.resolve(token);
            attributes.put(ImWebSocketAttributes.USER_ID, authenticatedUser.getUid());
            attributes.put(ImWebSocketAttributes.CLIENT_IP, clientIpResolver.resolve(request));
            handshakeSuccesses.increment();
            handshakeSuccessTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            return true;
        } catch (Exception ex) {
            recordHandshakeFailure(startNanos, handshakeInvalidTokenFailures);
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

    private void recordHandshakeFailure(long startNanos, Counter reasonCounter) {
        handshakeFailures.increment();
        reasonCounter.increment();
        handshakeFailureTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}
