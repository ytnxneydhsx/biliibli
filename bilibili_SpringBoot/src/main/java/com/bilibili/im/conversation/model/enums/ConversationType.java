package com.bilibili.im.conversation.model.enums;

import lombok.Getter;

@Getter
public enum ConversationType {

    SINGLE(1, "single"),
    GROUP(2, "group");

    private final int code;
    private final String description;

    ConversationType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
