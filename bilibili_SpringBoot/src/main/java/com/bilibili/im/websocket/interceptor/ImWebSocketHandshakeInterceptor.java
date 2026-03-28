package com.bilibili.im.websocket.interceptor;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.im.websocket.ImWebSocketAttributes;
import com.bilibili.security.AuthenticatedUserResolver;
import com.bilibili.security.TokenResolver;
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

    public ImWebSocketHandshakeInterceptor(TokenResolver tokenResolver,
                                           AuthenticatedUserResolver authenticatedUserResolver) {
        this.tokenResolver = tokenResolver;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = tokenResolver.resolve(request);
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            AuthenticatedUser authenticatedUser = authenticatedUserResolver.resolve(token);
            attributes.put(ImWebSocketAttributes.USER_ID, authenticatedUser.getUid());
            return true;
        } catch (Exception ex) {
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
}
