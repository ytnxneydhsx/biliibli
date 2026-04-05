package com.bilibili.im.group.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long groupId;
    private String conversationId;
    private String groupName;
    private Long ownerUserId;
    private String groupAvatar;
    private Integer status;
    private Integer memberCount;
    private Integer isAllMuted;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long lastServerMessageId;
    private Long lastMessageSeq;
}
