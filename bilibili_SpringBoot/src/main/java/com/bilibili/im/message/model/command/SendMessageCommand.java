package com.bilibili.im.message.model.command;

import com.bilibili.im.message.model.dto.MessageContentDTO;
import lombok.Data;

@Data
public class SendMessageCommand {

    private Long receiverId;

    private Long clientMessageId;

    private MessageContentDTO content;

    private Integer messageType;
}
