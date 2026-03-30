package com.bilibili.im.websocket.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationWindowUpdateDTO {

    private String conversationId;

    private Long targetUserId;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private Integer unreadCount;
}
