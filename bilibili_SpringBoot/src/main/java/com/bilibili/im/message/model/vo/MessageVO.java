package com.bilibili.im.message.model.vo;

import com.bilibili.im.message.model.dto.MessageContentDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;
    private String conversationId;
    private Long senderId;
    private Long receiverId;
    private Long clientMessageId;
    private String senderLocation;
    private Integer messageType;
    private MessageContentDTO content;
    private LocalDateTime sendTime;
    private Integer status;
}
