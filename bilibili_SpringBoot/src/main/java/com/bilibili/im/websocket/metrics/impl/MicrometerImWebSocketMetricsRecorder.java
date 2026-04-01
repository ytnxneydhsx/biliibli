package com.bilibili.im.websocket.metrics.impl;

import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.im.websocket.session.ImWebSocketSessionRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class MicrometerImWebSocketMetricsRecorder implements ImWebSocketMetricsRecorder {

    private final Counter handshakeAttempts;
    private final Counter handshakeSuccesses;
    private final Timer handshakeSuccessTimer;
    private final Timer handshakeFailureTimer;
    private final Map<String, Counter> handshakeFailureCounters = new ConcurrentHashMap<>();
    private final Counter connectionOpenedCounter;
    private final Map<String, Counter> connectionClosedCounters = new ConcurrentHashMap<>();
    private final Counter heartbeatReceivedCounter;
    private final Counter heartbeatAckSentCounter;
    private final Counter heartbeatAckFailedCounter;
    private final Counter inboundPayloadInvalidCounter;
    private final Counter inboundTypeInvalidCounter;
    private final Counter inboundTypeUnsupportedCounter;
    private final Counter expiredSessionCleanupCounter;
    private final MeterRegistry meterRegistry;

    public MicrometerImWebSocketMetricsRecorder(MeterRegistry meterRegistry,
                                                ImWebSocketSessionRegistry sessionRegistry) {
        this.meterRegistry = meterRegistry;
        this.handshakeAttempts = Counter.builder("im.ws.handshake.attempts")
                .description("Total websocket handshake attempts")
                .register(meterRegistry);
        this.handshakeSuccesses = Counter.builder("im.ws.handshake.success")
                .description("Successful websocket handshakes")
                .register(meterRegistry);
        this.handshakeSuccessTimer = Timer.builder("im.ws.handshake.duration")
                .description("Websocket handshake duration")
                .tag("outcome", "success")
                .register(meterRegistry);
        this.handshakeFailureTimer = Timer.builder("im.ws.handshake.duration")
                .description("Websocket handshake duration")
                .tag("outcome", "failure")
                .register(meterRegistry);

        this.connectionOpenedCounter = Counter.builder("im.ws.connection.opened")
                .description("Opened websocket connections")
                .register(meterRegistry);
        this.heartbeatReceivedCounter = Counter.builder("im.ws.heartbeat.received")
                .description("Received websocket heartbeats")
                .register(meterRegistry);
        this.heartbeatAckSentCounter = Counter.builder("im.ws.heartbeat.ack.sent")
                .description("Sent websocket heartbeat acknowledgements")
                .register(meterRegistry);
        this.heartbeatAckFailedCounter = Counter.builder("im.ws.heartbeat.ack.failed")
                .description("Failed websocket heartbeat acknowledgements")
                .register(meterRegistry);
        this.inboundPayloadInvalidCounter = Counter.builder("im.ws.inbound.payload.invalid")
                .description("Invalid websocket inbound payloads")
                .register(meterRegistry);
        this.inboundTypeInvalidCounter = Counter.builder("im.ws.inbound.type.invalid")
                .description("Invalid websocket inbound message types")
                .register(meterRegistry);
        this.inboundTypeUnsupportedCounter = Counter.builder("im.ws.inbound.type.unsupported")
                .description("Unsupported websocket inbound message types")
                .register(meterRegistry);
        this.expiredSessionCleanupCounter = Counter.builder("im.ws.cleanup.expired_sessions")
                .description("Expired websocket sessions removed by cleanup")
                .register(meterRegistry);

        Gauge.builder("im.ws.sessions.active", sessionRegistry, ImWebSocketSessionRegistry::countOpenSessions)
                .description("Current active websocket sessions")
                .register(meterRegistry);
        Gauge.builder("im.ws.users.online", sessionRegistry, ImWebSocketSessionRegistry::countOnlineUsers)
                .description("Current online websocket users")
                .register(meterRegistry);
    }

    @Override
    public void recordHandshakeAttempt() {
        handshakeAttempts.increment();
    }

    @Override
    public void recordHandshakeSuccess(long durationNanos) {
        handshakeSuccesses.increment();
        handshakeSuccessTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordHandshakeFailure(String reason, long durationNanos) {
        handshakeFailureCounter(reason).increment();
        handshakeFailureTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordConnectionOpened() {
        connectionOpenedCounter.increment();
    }

    @Override
    public void recordConnectionClosed(String reason) {
        connectionClosedCounter(reason).increment();
    }

    @Override
    public void recordHeartbeatReceived() {
        heartbeatReceivedCounter.increment();
    }

    @Override
    public void recordHeartbeatAckSent() {
        heartbeatAckSentCounter.increment();
    }

    @Override
    public void recordHeartbeatAckFailed() {
        heartbeatAckFailedCounter.increment();
    }

    @Override
    public void recordInboundPayloadInvalid() {
        inboundPayloadInvalidCounter.increment();
    }

    @Override
    public void recordInboundTypeInvalid() {
        inboundTypeInvalidCounter.increment();
    }

    @Override
    public void recordInboundTypeUnsupported() {
        inboundTypeUnsupportedCounter.increment();
    }

    @Override
    public void recordExpiredSessionCleanup(int expiredSessionCount) {
        if (expiredSessionCount > 0) {
            expiredSessionCleanupCounter.increment(expiredSessionCount);
        }
    }

    private Counter handshakeFailureCounter(String reason) {
        String normalizedReason = normalizeTagValue(reason, "unknown");
        return handshakeFailureCounters.computeIfAbsent(normalizedReason, key ->
                Counter.builder("im.ws.handshake.failure")
                        .description("Failed websocket handshakes")
                        .tag("reason", key)
                        .register(meterRegistry)
        );
    }

    private Counter connectionClosedCounter(String reason) {
        String normalizedReason = normalizeTagValue(reason, "unknown");
        return connectionClosedCounters.computeIfAbsent(normalizedReason, key ->
                Counter.builder("im.ws.connection.closed")
                        .description("Closed websocket connections")
                        .tag("reason", key)
                        .register(meterRegistry)
        );
    }

    private String normalizeTagValue(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim().toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("_+", "_");
    }
}
