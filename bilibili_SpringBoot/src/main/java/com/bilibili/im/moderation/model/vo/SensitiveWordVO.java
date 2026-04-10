package com.bilibili.im.moderation.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SensitiveWordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String word;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
