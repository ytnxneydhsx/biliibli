package com.bilibili.im.group.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateChatGroupMemberRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "role cannot be null")
    private Integer role;
}
