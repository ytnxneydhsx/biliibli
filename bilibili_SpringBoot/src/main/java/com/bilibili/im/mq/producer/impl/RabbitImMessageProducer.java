package com.bilibili.im.mq.producer.impl;

import com.bilibili.config.properties.ImMqProperties;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.metrics.ImSendMetrics;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.producer.ImMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RabbitImMessageProducer implements ImMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitImMessageProducer.class);
    private static final long CONFIRM_TIMEOUT_MILLIS = 5_000;
    private static final int MAX_CONFIRM_ATTEMPTS = 3;

    private final RabbitTemplate rabbitTemplate;
    private final ImMqProperties imMqProperties;
    private final ImSendMetrics imSendMetrics;
    private final ConcurrentMap<String, PendingConfirm> pendingConfirms = new ConcurrentHashMap<>();

    public RabbitImMessageProducer(RabbitTemplate rabbitTemplate,
                                   ImMqProperties imMqProperties,
                                   ImSendMetrics imSendMetrics) {
        this.rabbitTemplate = rabbitTemplate;
        this.imMqProperties = imMqProperties;
        this.imSendMetrics = imSendMetrics;
        this.rabbitTemplate.setMandatory(true);
        this.rabbitTemplate.setReturnsCallback(returned ->
                log.error("message returned: exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText())
        );
    }

    @Override
    public void publish(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        String routingKey = resolveRoutingKey(event);
        PendingConfirm pendingConfirm = new PendingConfirm(UUID.randomUUID().toString(), routingKey, event);
        pendingConfirms.put(pendingConfirm.correlationId(), pendingConfirm);
        updatePendingConfirmGauge();

        try {
            sendAttempt(pendingConfirm, true);
        } catch (RuntimeException ex) {
            pendingConfirms.remove(pendingConfirm.correlationId());
            updatePendingConfirmGauge();
            throw ex;
        }
    }

    @Scheduled(fixedDelay = 1_000)
    public void retryTimedOutConfirms() {
        long nowNanos = System.nanoTime();
        for (PendingConfirm pendingConfirm : pendingConfirms.values()) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(nowNanos - pendingConfirm.lastSentNanos());
            if (elapsedMillis < CONFIRM_TIMEOUT_MILLIS) {
                continue;
            }
            imSendMetrics.recordMqPublishConfirmTimeout();
            if (pendingConfirm.attempts() >= MAX_CONFIRM_ATTEMPTS) {
                if (pendingConfirms.remove(pendingConfirm.correlationId(), pendingConfirm)) {
                    imSendMetrics.recordMqPublishConfirmGiveUp();
                    updatePendingConfirmGauge();
                    log.error("publisher confirm timed out and gave up: correlationId={}, attempts={}, serverMessageId={}",
                            pendingConfirm.correlationId(),
                            pendingConfirm.attempts(),
                            pendingConfirm.event().getServerMessageId());
                }
                continue;
            }
            imSendMetrics.recordMqPublishConfirmRetry();
            log.warn("publisher confirm timed out; retrying: correlationId={}, attempts={}, serverMessageId={}",
                    pendingConfirm.correlationId(),
                    pendingConfirm.attempts(),
                    pendingConfirm.event().getServerMessageId());
            sendAttempt(pendingConfirm, false);
        }
    }

    private void sendAttempt(PendingConfirm pendingConfirm, boolean initialAttempt) {
        int attempt = pendingConfirm.nextAttempt();
        pendingConfirm.markSent();
        CorrelationData correlationData = new CorrelationData(pendingConfirm.correlationId());
        long publishStartNanos = System.nanoTime();
        try {
            long sendStartNanos = System.nanoTime();
            try {
                rabbitTemplate.convertAndSend(
                        imMqProperties.getExchange(),
                        pendingConfirm.routingKey(),
                        pendingConfirm.event(),
                        correlationData
                );
            } finally {
                imSendMetrics.recordMqPublishSend(System.nanoTime() - sendStartNanos);
            }
            long sentNanos = pendingConfirm.lastSentNanos();
            correlationData.getFuture().whenComplete((confirm, ex) ->
                    handleConfirm(pendingConfirm.correlationId(), attempt, sentNanos, confirm, ex));
        } catch (RuntimeException ex) {
            if (initialAttempt) {
                throw ex;
            }
            log.error("publisher confirm retry send failed: correlationId={}, attempt={}, serverMessageId={}",
                    pendingConfirm.correlationId(),
                    attempt,
                    pendingConfirm.event().getServerMessageId(),
                    ex);
        } finally {
            imSendMetrics.recordMqPublishTotal(System.nanoTime() - publishStartNanos);
        }
    }

    private void handleConfirm(String correlationId,
                               int attempt,
                               long sentNanos,
                               CorrelationData.Confirm confirm,
                               Throwable ex) {
        imSendMetrics.recordMqPublishConfirm(System.nanoTime() - sentNanos);
        PendingConfirm pendingConfirm = pendingConfirms.get(correlationId);
        if (pendingConfirm == null) {
            return;
        }
        if (ex != null) {
            imSendMetrics.recordMqPublishConfirmTimeout();
            log.error("publisher confirm callback failed: correlationId={}, attempt={}, serverMessageId={}",
                    correlationId,
                    attempt,
                    pendingConfirm.event().getServerMessageId(),
                    ex);
            return;
        }
        if (confirm != null && confirm.isAck()) {
            if (pendingConfirms.remove(correlationId, pendingConfirm)) {
                imSendMetrics.recordMqPublishConfirmAck();
                updatePendingConfirmGauge();
            }
            return;
        }

        String reason = confirm != null ? confirm.getReason() : "no confirm received";
        imSendMetrics.recordMqPublishConfirmNack();
        if (pendingConfirm.attempts() >= MAX_CONFIRM_ATTEMPTS) {
            if (pendingConfirms.remove(correlationId, pendingConfirm)) {
                imSendMetrics.recordMqPublishConfirmGiveUp();
                updatePendingConfirmGauge();
            }
            log.error("publisher confirm nack and gave up: correlationId={}, attempt={}, reason={}, serverMessageId={}",
                    correlationId,
                    attempt,
                    reason,
                    pendingConfirm.event().getServerMessageId());
            return;
        }

        imSendMetrics.recordMqPublishConfirmRetry();
        log.warn("publisher confirm nack; retrying: correlationId={}, attempt={}, reason={}, serverMessageId={}",
                correlationId,
                attempt,
                reason,
                pendingConfirm.event().getServerMessageId());
        sendAttempt(pendingConfirm, false);
    }

    private void updatePendingConfirmGauge() {
        imSendMetrics.recordMqPublishConfirmPending(pendingConfirms.size());
    }

    private String resolveRoutingKey(ImMessageDispatchEvent event) {
        Integer conversationType = event.getConversationType();
        if (Integer.valueOf(ConversationType.SINGLE.getCode()).equals(conversationType)) {
            return imMqProperties.getSingleRoutingKey();
        }
        if (Integer.valueOf(ConversationType.GROUP.getCode()).equals(conversationType)) {
            return imMqProperties.getGroupRoutingKey();
        }
        throw new IllegalArgumentException("conversationType is invalid");
    }

    private static final class PendingConfirm {

        private final String correlationId;
        private final String routingKey;
        private final ImMessageDispatchEvent event;
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile long lastSentNanos = System.nanoTime();

        private PendingConfirm(String correlationId, String routingKey, ImMessageDispatchEvent event) {
            this.correlationId = correlationId;
            this.routingKey = routingKey;
            this.event = event;
        }

        private String correlationId() {
            return correlationId;
        }

        private String routingKey() {
            return routingKey;
        }

        private ImMessageDispatchEvent event() {
            return event;
        }

        private int attempts() {
            return attempts.get();
        }

        private int nextAttempt() {
            return attempts.incrementAndGet();
        }

        private void markSent() {
            lastSentNanos = System.nanoTime();
        }

        private long lastSentNanos() {
            return lastSentNanos;
        }
    }
}
