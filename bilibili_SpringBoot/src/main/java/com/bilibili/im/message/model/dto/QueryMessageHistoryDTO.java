package com.bilibili.im.message.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QueryMessageHistoryDTO {

    @NotNull(message = "peerUid is required")
    private Long peerUid;

    private Long beforeMessageId;

    @Min(value = 1, message = "pageSize must be at least 1")
    @Max(value = 100, message = "pageSize must be at most 100")
    private Integer pageSize = 20;
}
