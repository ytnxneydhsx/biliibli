package com.bilibili.im.message.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String conversationId;
    private Long senderId;
    private Long receiverId;
    private Long clientMessageId;
    private String senderLocation;
    private Integer messageType;
    private String content;
    private LocalDateTime sendTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
