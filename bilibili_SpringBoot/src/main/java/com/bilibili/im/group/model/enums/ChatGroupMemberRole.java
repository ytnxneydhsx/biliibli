package com.bilibili.im.group.model.enums;

import lombok.Getter;

@Getter
public enum ChatGroupMemberRole {

    OWNER(1, "owner"),
    ADMIN(2, "admin"),
    MEMBER(3, "member");

    private final int code;
    private final String description;

    ChatGroupMemberRole(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
