package com.bilibili.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String nickname;

    private String password;

    private String confirmPassword;
}
