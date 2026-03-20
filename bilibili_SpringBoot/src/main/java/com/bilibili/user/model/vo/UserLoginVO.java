package com.bilibili.user.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long uid;

    private String username;

    private String token;
}
