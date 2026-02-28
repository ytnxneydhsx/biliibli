package com.bilibili.config.security;

import org.springframework.http.HttpMethod;

public class SecurityRule {

    private final HttpMethod method;
    private final String pattern;
    private final AccessLevel accessLevel;

    public SecurityRule(HttpMethod method, String pattern, AccessLevel accessLevel) {
        this.method = method;
        this.pattern = pattern;
        this.accessLevel = accessLevel;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPattern() {
        return pattern;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }
}
