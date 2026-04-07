package com.bilibili.im.conversation.cache.group.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupConversationCardCacheValue {

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
}
