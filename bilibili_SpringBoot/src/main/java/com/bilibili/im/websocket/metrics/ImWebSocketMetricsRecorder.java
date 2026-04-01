package com.bilibili.im.websocket.metrics;

public interface ImWebSocketMetricsRecorder {

    void recordHandshakeAttempt();

    void recordHandshakeSuccess(long durationNanos);

    void recordHandshakeFailure(String reason, long durationNanos);

    void recordConnectionOpened();

    void recordConnectionClosed(String reason);

    void recordHeartbeatReceived();

    void recordHeartbeatAckSent();

    void recordHeartbeatAckFailed();

    void recordInboundPayloadInvalid();

    void recordInboundTypeInvalid();

    void recordInboundTypeUnsupported();

    void recordExpiredSessionCleanup(int expiredSessionCount);
}
