package com.bilibili.security.resolver.impl;

import com.bilibili.security.resolver.TokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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

        String tokenFromHeader = resolveTokenFromHeader(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (StringUtils.hasText(tokenFromHeader)) {
            return tokenFromHeader;
        }
        return resolveTokenFromQuery(request.getURI());
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
