package com.bilibili.user.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long uid;

    private String nickname;

    private String avatar;

    private String sign;

    private Integer followerCount;

    private Integer followingCount;
}
