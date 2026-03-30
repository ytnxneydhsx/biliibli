package com.bilibili.im.conversation.cache.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationWindowCacheValue {

    private String conversationId;
    private Long targetId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long lastServerMessageId;
    private Integer unreadCount;
    private Integer isMuted;
}
