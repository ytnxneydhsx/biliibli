package com.bilibili.im.group.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateChatGroupMuteStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "isMuted cannot be null")
    private Integer isMuted;
}
