package com.bilibili.im.mq;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;

public final class ImMqLogContext {

    private ImMqLogContext() {
    }

    public static LogContext.Scope open(ImMessageDispatchEvent event, String traceId, String uid) {
        return LogContext.open(traceId, resolveUid(event, uid));
    }

    private static Long resolveUid(ImMessageDispatchEvent event, String uid) {
        Long parsedUid = parseUid(uid);
        if (parsedUid != null && parsedUid > 0) {
            return parsedUid;
        }
        return event == null ? null : event.getSenderId();
    }

    private static Long parseUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(uid.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
