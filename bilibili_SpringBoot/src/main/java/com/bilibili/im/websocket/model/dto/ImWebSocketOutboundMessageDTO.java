package com.bilibili.im.websocket.model.dto;

import lombok.Data;

@Data
public class ImWebSocketOutboundMessageDTO {

    private String type;

    private Integer code;

    private String message;

    private Object data;
}
