package com.bilibili.im.message.model.vo;

import com.bilibili.im.message.model.dto.MessageContentDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SendMessageVO {

    private String conversationId;
    private Integer messageType;
    private MessageContentDTO content;
    private String senderLocation;
    private LocalDateTime sendTime;
    private Integer status;
}
