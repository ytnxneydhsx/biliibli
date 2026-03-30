package com.bilibili.security.resolver.impl;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.security.JwtTokenService;
import com.bilibili.security.resolver.AuthenticatedUserResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtAuthenticatedUserResolver implements AuthenticatedUserResolver {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticatedUserResolver(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthenticatedUser resolve(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token is invalid");
        }
        return jwtTokenService.parse(token);
    }
}
