package com.bilibili.controller.support;

import com.bilibili.common.auth.AuthenticatedUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test-only resolver for @AuthenticationPrincipal AuthenticatedUser.
 * It reads user id from header X-Test-Uid. If absent, resolves to null.
 */
public class TestAuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String UID_HEADER = "X-Test-Uid";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String uidRaw = webRequest.getHeader(UID_HEADER);
        if (uidRaw == null || uidRaw.trim().isEmpty()) {
            return null;
        }
        return new AuthenticatedUser(Long.parseLong(uidRaw.trim()));
    }
}
