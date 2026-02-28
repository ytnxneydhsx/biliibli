package com.bilibili.common.auth;

import java.io.Serializable;

public class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long uid;

    public AuthenticatedUser(Long uid) {
        this.uid = uid;
    }

    public Long getUid() {
        return uid;
    }
}
