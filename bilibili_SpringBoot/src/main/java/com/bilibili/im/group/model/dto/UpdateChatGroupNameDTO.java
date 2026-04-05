package com.bilibili.im.group.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateChatGroupNameDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "groupName cannot be blank")
    private String groupName;
}
