package com.bilibili.im.websocket.metrics.impl;

import com.bilibili.im.websocket.connection.ImConnectionRegistry;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
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
    private final Map<String, Timer> inboundHandleTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> inboundDecodeTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> protocolDispatchTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> protocolIdempotencyTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> protocolAcceptCallTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> outboundEncodeTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> outboundSendTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> pushSerializeTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> pushSendTimers = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public MicrometerImWebSocketMetricsRecorder(MeterRegistry meterRegistry,
                                                ImConnectionRegistry connectionRegistry) {
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

        Gauge.builder("im.ws.sessions.active", connectionRegistry, ImConnectionRegistry::countOpenConnections)
                .description("Current active websocket sessions")
                .register(meterRegistry);
        Gauge.builder("im.ws.users.online", connectionRegistry, ImConnectionRegistry::countOnlineUsers)
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

    @Override
    public void recordInboundHandle(String type, String outcome, long durationNanos) {
        taggedTimer(
                inboundHandleTimers,
                "im.ws.inbound.handle",
                "WebSocket inbound frame handling duration",
                normalizeTagValue(type, "unknown"),
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordInboundDecode(String outcome, long durationNanos) {
        taggedTimer(
                inboundDecodeTimers,
                "im.ws.inbound.decode",
                "WebSocket inbound JSON decode duration",
                "all",
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordProtocolDispatch(String type, String outcome, long durationNanos) {
        taggedTimer(
                protocolDispatchTimers,
                "im.ws.protocol.dispatch",
                "WebSocket protocol dispatch duration",
                normalizeTagValue(type, "unknown"),
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordProtocolIdempotency(String outcome, long durationNanos) {
        taggedTimer(
                protocolIdempotencyTimers,
                "im.ws.protocol.idempotency",
                "WebSocket send_message idempotency check duration",
                "send_message",
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordProtocolAcceptCall(String outcome, long durationNanos) {
        taggedTimer(
                protocolAcceptCallTimers,
                "im.ws.protocol.accept_call",
                "WebSocket send_message application accept call duration",
                "send_message",
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordOutboundEncode(String type, String outcome, long durationNanos) {
        taggedTimer(
                outboundEncodeTimers,
                "im.ws.outbound.encode",
                "WebSocket outbound JSON encode duration",
                normalizeTagValue(type, "unknown"),
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordOutboundSend(String type, String outcome, long durationNanos) {
        taggedTimer(
                outboundSendTimers,
                "im.ws.outbound.send",
                "WebSocket outbound send duration",
                normalizeTagValue(type, "unknown"),
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordPushSerialize(String type, String outcome, long durationNanos) {
        taggedTimer(
                pushSerializeTimers,
                "im.ws.push.serialize",
                "WebSocket push payload serialize duration",
                normalizeTagValue(type, "unknown"),
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordPushSend(String type, String outcome, long durationNanos) {
        taggedTimer(
                pushSendTimers,
                "im.ws.push.send",
                "WebSocket push send duration",
                normalizeTagValue(type, "unknown"),
                normalizeTagValue(outcome, "unknown")
        ).record(durationNanos, TimeUnit.NANOSECONDS);
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

    private Timer taggedTimer(Map<String, Timer> cache,
                              String name,
                              String description,
                              String type,
                              String outcome) {
        String key = type + "|" + outcome;
        return cache.computeIfAbsent(key, ignored ->
                Timer.builder(name)
                        .description(description)
                        .tag("type", type)
                        .tag("outcome", outcome)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(meterRegistry)
        );
    }
}
