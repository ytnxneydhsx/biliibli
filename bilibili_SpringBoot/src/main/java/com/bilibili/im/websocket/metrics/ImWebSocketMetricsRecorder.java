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

    void recordInboundHandle(String type, String outcome, long durationNanos);

    void recordInboundDecode(String outcome, long durationNanos);

    void recordProtocolDispatch(String type, String outcome, long durationNanos);

    void recordProtocolIdempotency(String outcome, long durationNanos);

    void recordProtocolAcceptCall(String outcome, long durationNanos);

    void recordOutboundEncode(String type, String outcome, long durationNanos);

    void recordOutboundSend(String type, String outcome, long durationNanos);

    void recordPushSerialize(String type, String outcome, long durationNanos);

    void recordPushSend(String type, String outcome, long durationNanos);
}
