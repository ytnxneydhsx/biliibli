package com.bilibili.im.group.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatGroupMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long serverMessageId;
    private Long groupMessageSeq;
    private LocalDateTime createTime;
}
