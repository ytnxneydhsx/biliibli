package com.bilibili.im.websocket.model.dto;

import com.bilibili.im.message.model.dto.MessageContentDTO;
import lombok.Data;

@Data
public class ImWebSocketInboundMessageDTO {

    private String type;

    private Integer conversationType;

    private Long receiverId;

    private Long clientMessageId;

    private Integer messageType;

    private MessageContentDTO content;
}
