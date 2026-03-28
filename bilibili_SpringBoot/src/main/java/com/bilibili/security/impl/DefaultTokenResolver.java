package com.bilibili.security.impl;

import com.bilibili.security.TokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class DefaultTokenResolver implements TokenResolver {

    @Override
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String tokenFromHeader = resolveTokenFromHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (StringUtils.hasText(tokenFromHeader)) {
            return tokenFromHeader;
        }
        return request.getParameter("token");
    }

    @Override
    public String resolve(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }

        String tokenFromHeader = resolveTokenFromHeaders(request.getHeaders().get(HttpHeaders.AUTHORIZATION));
        if (StringUtils.hasText(tokenFromHeader)) {
            return tokenFromHeader;
        }
        return resolveTokenFromQuery(request.getURI());
    }

    private String resolveTokenFromHeaders(List<String> authHeaders) {
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        return resolveTokenFromHeader(authHeaders.get(0));
    }

    private String resolveTokenFromHeader(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private String resolveTokenFromQuery(URI uri) {
        if (uri == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst("token");
    }
}
