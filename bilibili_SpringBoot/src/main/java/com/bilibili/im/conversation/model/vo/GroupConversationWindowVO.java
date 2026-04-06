package com.bilibili.im.conversation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupConversationWindowVO {

    private String conversationId;
    private Long groupId;
    private String groupName;
    private String groupAvatar;
    private Integer status;
    private Integer memberCount;
    private Integer isAllMuted;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long lastServerMessageId;
    private Long lastMessageSeq;
    private Long lastReadSeq;
    private Integer unreadCount;
    private Integer isMuted;
}
