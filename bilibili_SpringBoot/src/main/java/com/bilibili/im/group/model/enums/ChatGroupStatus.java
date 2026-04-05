package com.bilibili.im.group.model.enums;

import lombok.Getter;

@Getter
public enum ChatGroupStatus {

    ACTIVE(1, "active"),
    DISMISSED(2, "dismissed");

    private final int code;
    private final String description;

    ChatGroupStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
