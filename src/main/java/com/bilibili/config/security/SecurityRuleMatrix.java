package com.bilibili.config.security;

import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecurityRuleMatrix {

    private static final List<SecurityRule> RULES = buildRules();

    private SecurityRuleMatrix() {
    }

    public static List<SecurityRule> getRules() {
        return RULES;
    }

    private static List<SecurityRule> buildRules() {
        List<SecurityRule> rules = new ArrayList<>();

        // Public endpoints.
        for (String path : ApiPaths.AUTH_FREE) {
            addRule(rules, null, path, AccessLevel.PUBLIC);
        }
        addRule(rules, HttpMethod.OPTIONS, "/**", AccessLevel.PUBLIC);
        for (String path : ApiPaths.PUBLIC_GET) {
            addRule(rules, HttpMethod.GET, path, AccessLevel.PUBLIC);
        }
        for (String path : ApiPaths.PUBLIC_POST) {
            addRule(rules, HttpMethod.POST, path, AccessLevel.PUBLIC);
        }

        // Authenticated endpoints.
        for (String path : ApiPaths.USER_ONLY) {
            addRule(rules, null, path, AccessLevel.AUTH);
        }
        addRule(rules, HttpMethod.POST, ApiPaths.USER_LOGOUT, AccessLevel.AUTH);

        return Collections.unmodifiableList(rules);
    }

    private static void addRule(List<SecurityRule> rules, HttpMethod method, String path, AccessLevel accessLevel) {
        rules.add(new SecurityRule(method, path, accessLevel));
    }
}
