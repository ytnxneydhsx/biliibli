package com.bilibili.im.privacy.model.vo;

import lombok.Data;

@Data
public class UserPrivacySettingVO {

    private Long userId;
    private Integer privateMessagePolicy;
}
