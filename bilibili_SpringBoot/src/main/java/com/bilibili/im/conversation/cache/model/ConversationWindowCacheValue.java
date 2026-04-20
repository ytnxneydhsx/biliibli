package com.bilibili.im.conversation.cache.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationWindowCacheValue {

    private String conversationId;
    private Long targetId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long lastServerMessageId;
    private String unreadBaselineServerMessageIdText;
    private Integer unreadCount;
    private Integer isMuted;
}
