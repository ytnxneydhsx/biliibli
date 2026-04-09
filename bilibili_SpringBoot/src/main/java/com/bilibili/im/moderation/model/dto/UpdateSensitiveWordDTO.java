package com.bilibili.im.moderation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateSensitiveWordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "word cannot be blank")
    @Size(max = 255, message = "word length must be less than or equal to 255")
    private String word;
}
