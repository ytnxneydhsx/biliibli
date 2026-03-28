package com.bilibili.access.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserAccessDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Integer likeEnabled;

    private Integer commentEnabled;

    private Integer imMessageSendEnabled;

    private Integer videoUploadEnabled;

    private Integer profileEditEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
