package com.bilibili.im.contact.model.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ContactRelationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long targetUserId;
    private Integer isContact;
    private Integer isBlocked;
    private Integer isMuted;
}
