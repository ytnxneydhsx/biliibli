package com.bilibili.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nickname;

    private String sign;
}
