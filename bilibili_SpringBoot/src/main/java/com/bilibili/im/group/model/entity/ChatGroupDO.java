package com.bilibili.im.group.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatGroupDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
