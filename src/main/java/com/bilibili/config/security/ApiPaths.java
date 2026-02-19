package com.bilibili.config.security;

public final class ApiPaths {

    public static final String[] AUTH_FREE = {
            "/users/login",
            "/users/register"
    };

    public static final String[] PUBLIC_GET = {
            "/users/*",
            "/users/*/followers",
            "/users/*/followings",
            "/users/*/friends"
    };

    public static final String[] USER_ONLY = {
            "/me/**"
    };

    public static final String USER_LOGOUT = "/users/logout";

    private ApiPaths() {
    }
}

