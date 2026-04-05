package com.bilibili.im.conversation.model.enums;

import lombok.Getter;

@Getter
public enum ChatGroupConversationStatus {

    ACTIVE(1, "active"),
    HIDDEN_AFTER_EXIT(2, "hidden_after_exit");

    private final int code;
    private final String description;

    ChatGroupConversationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
