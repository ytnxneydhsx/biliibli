package com.bilibili.common.enums;

import java.util.Objects;

public enum UserStatus {
    NORMAL(0);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean matches(Integer value) {
        return Objects.equals(value, code);
    }
}
