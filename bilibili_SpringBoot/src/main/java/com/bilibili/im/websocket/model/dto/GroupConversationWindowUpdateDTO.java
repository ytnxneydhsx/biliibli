package com.bilibili.im.websocket.model.dto;

import lombok.Data;

@Data
public class GroupConversationWindowUpdateDTO {

    private Long groupId;

    private Long lastServerMessageId;
}
