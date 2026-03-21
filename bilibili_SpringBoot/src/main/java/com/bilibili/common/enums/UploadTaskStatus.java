package com.bilibili.common.enums;

import java.util.Objects;

public enum UploadTaskStatus {
    UPLOADING(0),
    COMPLETING(1),
    DONE(2),
    EXPIRED(3),
    FAILED(4),
    CANCELLED(5);

    private final int code;

    UploadTaskStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean matches(Integer value) {
        return Objects.equals(value, code);
    }
}
