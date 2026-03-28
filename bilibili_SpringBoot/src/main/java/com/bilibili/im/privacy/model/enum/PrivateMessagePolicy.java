package com.bilibili.im.privacy.model.enum;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum PrivateMessagePolicy {

    ALLOW_ALL(1, "allow_all"),
    CONTACT_ONLY(2, "contact_only"),
    STRANGER_FIRST_MESSAGE_ONLY(3, "stranger_first_message_only"),
    DENY_ALL(4, "deny_all");

    private final int code;
    private final String description;

    PrivateMessagePolicy(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PrivateMessagePolicy fromCode(Integer code) {
        if (code == null) {
            return ALLOW_ALL;
        }
        return Arrays.stream(values())
                .filter(policy -> policy.code == code)
                .findFirst()
                .orElse(ALLOW_ALL);
    }

    public static boolean supports(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(policy -> policy.code == code);
    }
}
