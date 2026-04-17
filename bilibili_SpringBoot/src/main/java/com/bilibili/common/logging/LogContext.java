package com.bilibili.common.logging;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public final class LogContext {

    public static final String TRACE_ID = "traceId";
    public static final String UID = "uid";
    public static final String TRACE_ID_HEADER = "x-trace-id";
    public static final String UID_HEADER = "x-uid";

    private LogContext() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String currentTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static String currentUid() {
        return MDC.get(UID);
    }

    public static void putUid(Long uid) {
        if (uid == null || uid <= 0) {
            MDC.remove(UID);
            return;
        }
        MDC.put(UID, String.valueOf(uid));
    }

    public static Scope open(String traceId, Long uid) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String resolvedTraceId = isBlank(traceId) ? newTraceId() : traceId.trim();
        MDC.put(TRACE_ID, resolvedTraceId);
        putUid(uid);
        return new Scope(previousContext);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class Scope implements AutoCloseable {

        private final Map<String, String> previousContext;

        private Scope(Map<String, String> previousContext) {
            this.previousContext = previousContext;
        }

        @Override
        public void close() {
            if (previousContext == null || previousContext.isEmpty()) {
                MDC.clear();
                return;
            }
            MDC.setContextMap(previousContext);
        }
    }
}
