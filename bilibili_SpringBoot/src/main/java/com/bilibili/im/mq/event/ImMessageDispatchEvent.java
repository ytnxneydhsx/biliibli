package com.bilibili.im.mq.event;

import com.bilibili.im.message.model.dto.MessageContentDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImMessageDispatchEvent {

    private String conversationId;
    private Long senderId;
    private Long receiverId;
    private Integer messageType;
    private MessageContentDTO content;
    private LocalDateTime sendTime;
    private Long clientMessageId;
    private String senderLocation;
}
