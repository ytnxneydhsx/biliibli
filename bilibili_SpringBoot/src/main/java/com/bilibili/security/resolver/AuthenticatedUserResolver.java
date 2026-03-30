package com.bilibili.security.resolver;

import com.bilibili.common.auth.AuthenticatedUser;

public interface AuthenticatedUserResolver {

    AuthenticatedUser resolve(String token);
}
