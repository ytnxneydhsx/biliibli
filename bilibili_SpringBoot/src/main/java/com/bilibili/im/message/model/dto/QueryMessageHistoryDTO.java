package com.bilibili.im.message.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QueryMessageHistoryDTO {

    @NotNull(message = "peerUid is required")
    private Long peerUid;

    private Long beforeServerMessageId;
}
