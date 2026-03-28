package com.bilibili.im.conversation.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatConversationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String conversationId;
    private Long ownerUserId;
    private Long targetId;
    private Integer type;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer isMuted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
