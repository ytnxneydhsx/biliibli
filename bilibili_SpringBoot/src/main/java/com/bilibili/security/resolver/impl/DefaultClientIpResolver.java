package com.bilibili.security.resolver.impl;

import com.bilibili.security.resolver.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

@Component
public class DefaultClientIpResolver implements ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    @Override
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (hasText(xForwardedFor)) {
            return extractFirstIp(xForwardedFor);
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (hasText(xRealIp)) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return hasText(remoteAddr) ? remoteAddr.trim() : null;
    }

    @Override
    public String resolve(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }

        HttpHeaders headers = request.getHeaders();
        List<String> xForwardedFor = headers.get("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && hasText(xForwardedFor.get(0))) {
            return extractFirstIp(xForwardedFor.get(0));
        }

        String xRealIp = headers.getFirst("X-Real-IP");
        if (hasText(xRealIp)) {
            return xRealIp.trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return null;
        }
        String hostAddress = remoteAddress.getAddress().getHostAddress();
        return hasText(hostAddress) ? hostAddress.trim() : null;
    }

    private String extractFirstIp(String xForwardedFor) {
        if (!hasText(xForwardedFor)) {
            return null;
        }
        String[] segments = xForwardedFor.split(",");
        for (String segment : segments) {
            String ip = segment == null ? null : segment.trim();
            if (hasText(ip)) {
                return ip;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value.trim());
    }
}
