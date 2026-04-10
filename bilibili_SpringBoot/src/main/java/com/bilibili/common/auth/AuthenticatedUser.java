package com.bilibili.common.auth;

import com.bilibili.common.enums.UserRole;

import java.io.Serializable;

public class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long uid;
    private final UserRole role;

    public AuthenticatedUser(Long uid) {
        this(uid, UserRole.USER);
    }

    public AuthenticatedUser(Long uid, UserRole role) {
        this.uid = uid;
        this.role = role == null ? UserRole.USER : role;
    }

    public Long getUid() {
        return uid;
    }

    public UserRole getRole() {
        return role;
    }

    public Integer getRoleCode() {
        return role.code();
    }
}
