package com.bilibili.im.group.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatGroupMemberDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;
    private Integer status;
    private Integer isMuted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
