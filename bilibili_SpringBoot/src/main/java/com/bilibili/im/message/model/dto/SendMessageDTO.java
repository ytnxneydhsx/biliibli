package com.bilibili.im.message.model.dto;

import com.bilibili.im.conversation.model.enums.ConversationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageDTO {

    /**
     * Optional for backward compatibility.
     * Defaults to SINGLE when omitted.
     */
    private Integer conversationType = ConversationType.SINGLE.getCode();

    @NotNull(message = "receiverId cannot be null")
    private Long receiverId;

    @NotNull(message = "clientMessageId cannot be null")
    private Long clientMessageId;

    @Valid
    @NotNull(message = "content cannot be null")
    private MessageContentDTO content;

    @NotNull(message = "messageType cannot be null")
    private Integer messageType;
}
