package com.bilibili.im.group.model.enums;

import lombok.Getter;

@Getter
public enum ChatGroupMemberStatus {

    ACTIVE(1, "active"),
    LEFT(2, "left"),
    REMOVED(3, "removed");

    private final int code;
    private final String description;

    ChatGroupMemberStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
