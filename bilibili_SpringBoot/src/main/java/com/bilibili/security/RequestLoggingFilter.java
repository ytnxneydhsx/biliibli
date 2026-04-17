package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.logging.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern SENSITIVE_QUERY_PARAM = Pattern.compile("(?i)(^|&)([^=&]*token[^=&]*=)[^&]*");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String traceId = LogContext.newTraceId();
        response.setHeader("X-Trace-Id", traceId);
        LogContext.Scope logScope = LogContext.open(traceId, null);
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                Long uid = resolveUid();
                LogContext.putUid(uid);

                long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                String path = buildSafeRequestPath(request);

                log.info("request method={} path={} status={} costMs={} uid={}",
                        request.getMethod(), path, response.getStatus(), costMs, uid == null ? "-" : uid);
            } finally {
                logScope.close();
            }
        }
    }

    private static String buildSafeRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return path;
        }
        return path + "?" + SENSITIVE_QUERY_PARAM.matcher(queryString).replaceAll("$1$2***");
    }

    private static Long resolveUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) principal).getUid();
        }
        return null;
    }
}
