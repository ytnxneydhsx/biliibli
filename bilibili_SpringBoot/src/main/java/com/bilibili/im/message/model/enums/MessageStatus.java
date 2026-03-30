package com.bilibili.im.message.model.enums;

import lombok.Getter;

@Getter
public enum MessageStatus {

    ACCEPTED(0, "accepted"),
    SUCCESS(1, "success"),
    FAILED(2, "failed"),
    REVOKED(3, "revoked");

    private final int code;
    private final String description;

    MessageStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
