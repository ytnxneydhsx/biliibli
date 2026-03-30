package com.bilibili.security.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;

public interface ClientIpResolver {

    String resolve(HttpServletRequest request);

    String resolve(ServerHttpRequest request);
}
