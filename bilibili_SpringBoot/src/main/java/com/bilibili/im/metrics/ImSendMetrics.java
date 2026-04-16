package com.bilibili.im.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ImSendMetrics {

    private static final Logger log = LoggerFactory.getLogger(ImSendMetrics.class);
    private static final long SLOW_ACCEPT_THRESHOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(1000);
    private static final long SLOW_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final Timer acceptTotalTimer;
    private final Timer acceptValidationTimer;
    private final Timer acceptConversationTimer;
    private final Timer acceptLocationTimer;
    private final Timer acceptPublishTimer;
    private final Timer mqPublishTotalTimer;
    private final Timer mqPublishSendTimer;
    private final Timer mqPublishConfirmTimer;
    private final Counter mqPublishConfirmNackCounter;
    private final Counter mqPublishConfirmTimeoutCounter;
    private final AtomicLong lastSlowAcceptLogNanos = new AtomicLong();

    public ImSendMetrics(MeterRegistry meterRegistry) {
        this.acceptTotalTimer = timer(meterRegistry, "im.send.accept.total",
                "Total send_message accept processing duration");
        this.acceptValidationTimer = timer(meterRegistry, "im.send.accept.validation",
                "send_message validation duration");
        this.acceptConversationTimer = timer(meterRegistry, "im.send.accept.conversation",
                "send_message conversation resolution duration");
        this.acceptLocationTimer = timer(meterRegistry, "im.send.accept.location",
                "send_message sender location resolution duration");
        this.acceptPublishTimer = timer(meterRegistry, "im.send.accept.publish",
                "send_message MQ publish call duration inside accept flow");
        this.mqPublishTotalTimer = timer(meterRegistry, "im.mq.publish.total",
                "Total RabbitMQ publish duration");
        this.mqPublishSendTimer = timer(meterRegistry, "im.mq.publish.send",
                "RabbitMQ convertAndSend duration");
        this.mqPublishConfirmTimer = timer(meterRegistry, "im.mq.publish.confirm",
                "RabbitMQ publisher confirm wait duration");
        this.mqPublishConfirmNackCounter = Counter.builder("im.mq.publish.confirm.nack")
                .description("RabbitMQ publisher confirm nack count")
                .register(meterRegistry);
        this.mqPublishConfirmTimeoutCounter = Counter.builder("im.mq.publish.confirm.timeout")
                .description("RabbitMQ publisher confirm timeout or wait failure count")
                .register(meterRegistry);
    }

    public void recordAcceptTotal(long durationNanos) {
        acceptTotalTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordAcceptValidation(long durationNanos) {
        acceptValidationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordAcceptConversation(long durationNanos) {
        acceptConversationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordAcceptLocation(long durationNanos) {
        acceptLocationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordAcceptPublish(long durationNanos) {
        acceptPublishTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordMqPublishTotal(long durationNanos) {
        mqPublishTotalTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordMqPublishSend(long durationNanos) {
        mqPublishSendTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordMqPublishConfirm(long durationNanos) {
        mqPublishConfirmTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordMqPublishConfirmNack() {
        mqPublishConfirmNackCounter.increment();
    }

    public void recordMqPublishConfirmTimeout() {
        mqPublishConfirmTimeoutCounter.increment();
    }

    public void recordSlowAccept(Long senderId,
                                 Long clientMessageId,
                                 long totalNanos,
                                 long validationNanos,
                                 long conversationNanos,
                                 long locationNanos,
                                 long publishNanos) {
        if (totalNanos < SLOW_ACCEPT_THRESHOLD_NANOS) {
            return;
        }
        long now = System.nanoTime();
        long last = lastSlowAcceptLogNanos.get();
        if (now - last < SLOW_LOG_INTERVAL_NANOS
                || !lastSlowAcceptLogNanos.compareAndSet(last, now)) {
            return;
        }
        log.warn("im send accept slow senderId={} clientMessageId={} totalMs={} validationMs={} conversationMs={} locationMs={} publishMs={}",
                senderId,
                clientMessageId,
                toMillis(totalNanos),
                toMillis(validationNanos),
                toMillis(conversationNanos),
                toMillis(locationNanos),
                toMillis(publishNanos)
        );
    }

    private Timer timer(MeterRegistry meterRegistry, String name, String description) {
        return Timer.builder(name)
                .description(description)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private long toMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(nanos, 0));
    }
}
