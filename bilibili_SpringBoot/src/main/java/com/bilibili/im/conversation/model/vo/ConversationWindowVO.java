package com.bilibili.im.conversation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationWindowVO {

    private String conversationId;
    private Long targetId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer isMuted;
}
