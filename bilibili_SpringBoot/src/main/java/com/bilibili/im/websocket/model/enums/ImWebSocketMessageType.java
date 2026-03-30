package com.bilibili.im.websocket.model.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ImWebSocketMessageType {

    HEARTBEAT("heartbeat"),
    HEARTBEAT_ACK("heartbeat_ack"),
    SEND_MESSAGE("send_message"),
    SEND_MESSAGE_ACCEPTED("send_message_accepted"),
    MESSAGE_RECEIVED("message_received"),
    CONVERSATION_UPDATED("conversation_updated"),
    ERROR("error");

    private final String code;

    ImWebSocketMessageType(String code) {
        this.code = code;
    }

    public static boolean matches(String rawType, ImWebSocketMessageType expectedType) {
        if (rawType == null || rawType.isBlank() || expectedType == null) {
            return false;
        }
        return expectedType.code.equalsIgnoreCase(rawType);
    }

    public static ImWebSocketMessageType fromCode(String rawType) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(rawType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("websocket message type is unsupported"));
    }
}
