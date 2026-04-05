package com.bilibili.im.group.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

@Data
public class InviteGroupMemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "targetUserId cannot be null")
    @Positive(message = "targetUserId must be positive")
    private Long targetUserId;
}
