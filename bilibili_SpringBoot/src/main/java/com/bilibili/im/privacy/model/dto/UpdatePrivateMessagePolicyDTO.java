package com.bilibili.im.privacy.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePrivateMessagePolicyDTO {

    @NotNull(message = "privateMessagePolicy cannot be null")
    private Integer privateMessagePolicy;
}
