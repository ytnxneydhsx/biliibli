package com.bilibili.im.group.model.enums;

import lombok.Getter;

@Getter
public enum ChatGroupMuteStatus {

    UNMUTED(0, "unmuted"),
    MUTED(1, "muted");

    private final int code;
    private final String description;

    ChatGroupMuteStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
