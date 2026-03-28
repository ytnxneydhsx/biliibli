package com.bilibili.im.message.model.enum;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MessageType {

    TEXT(1, "text"),
    IMAGE(2, "image"),
    RICH(3, "rich");

    private final int code;
    private final String description;

    MessageType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean supports(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(type -> type.code == code);
    }

    public static MessageType fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("messageType is invalid"));
    }
}
