package com.bilibili.im.group.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatGroupMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Integer role;
    private Integer status;
    private Integer isMuted;
    private Long lastReadSeq;
}
