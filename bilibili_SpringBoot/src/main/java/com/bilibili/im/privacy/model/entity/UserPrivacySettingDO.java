package com.bilibili.im.privacy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserPrivacySettingDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Integer privateMessagePolicy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
