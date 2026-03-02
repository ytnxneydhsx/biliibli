package com.bilibili.common.enums;

import java.util.Objects;

public enum RecordStatus {
    NORMAL(0),
    DELETED(1);

    private final int code;

    RecordStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean matches(Integer value) {
        return Objects.equals(value, code);
    }
}
