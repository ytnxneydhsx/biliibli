package com.bilibili.security;

import com.bilibili.common.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_ID = "traceId";
    private static final String UID = "uid";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String traceId = resolveTraceId();

        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            Long uid = resolveUid();
            if (uid != null) {
                MDC.put(UID, String.valueOf(uid));
            }

            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            String path = request.getRequestURI();
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                path = path + "?" + request.getQueryString();
            }

            log.info("request method={} path={} status={} costMs={} uid={}",
                    request.getMethod(), path, response.getStatus(), costMs, uid == null ? "-" : uid);

            MDC.remove(UID);
            MDC.remove(TRACE_ID);
        }
    }

    private static String resolveTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
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
