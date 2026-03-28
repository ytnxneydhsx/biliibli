package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;

public interface AuthenticatedUserResolver {

    AuthenticatedUser resolve(String token);
}
