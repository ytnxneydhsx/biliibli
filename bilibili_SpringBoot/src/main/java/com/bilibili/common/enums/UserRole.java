package com.bilibili.common.enums;

import java.util.Objects;

public enum UserRole {
    USER(0),
    ADMIN(1);

    public static final String ROLE_CODE_CLAIM = "role_code";

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean matches(Integer value) {
        return Objects.equals(value, code);
    }

    public static UserRole defaultRole() {
        return USER;
    }

    public static UserRole fromCodeOrDefault(Integer value) {
        return fromCodeOrDefault(value, defaultRole());
    }

    public static UserRole fromCodeOrDefault(Integer value, UserRole defaultRole) {
        for (UserRole role : values()) {
            if (role.matches(value)) {
                return role;
            }
        }
        return defaultRole;
    }
}
