package com.bilibili.user.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long uid;

    private String nickname;

    private String avatar;

    private String sign;

    private Integer followerCount;

    private Integer followingCount;
}
