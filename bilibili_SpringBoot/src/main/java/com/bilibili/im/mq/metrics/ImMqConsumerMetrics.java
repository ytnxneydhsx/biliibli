package com.bilibili.im.mq.metrics;

import com.bilibili.config.properties.ImMqProperties;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ImMqConsumerMetrics {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";

    private final MeterRegistry meterRegistry;
    private final ImMqProperties imMqProperties;

    public ImMqConsumerMetrics(MeterRegistry meterRegistry, ImMqProperties imMqProperties) {
        this.meterRegistry = meterRegistry;
        this.imMqProperties = imMqProperties;
    }

    public void record(Consumer consumer, ImMessageDispatchEvent event, Runnable action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = STATUS_FAILURE;
        try {
            action.run();
            status = STATUS_SUCCESS;
        } catch (RuntimeException | Error ex) {
            incrementError(consumer, ex);
            throw ex;
        } finally {
            String queue = queueName(consumer);
            Tags tags = Tags.of(
                    "consumer", consumer.tag,
                    "queue", queue,
                    "status", status
            );

            Counter.builder("im.mq.consumer.messages")
                    .description("IM MQ consumer processed message count")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder("im.mq.consumer.duration")
                    .description("IM MQ consumer processing duration")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            recordLag(consumer, queue, status, event);
        }
    }

    private void incrementError(Consumer consumer, Throwable ex) {
        Counter.builder("im.mq.consumer.errors")
                .description("IM MQ consumer error count")
                .tags(
                        "consumer", consumer.tag,
                        "queue", queueName(consumer),
                        "exception", ex.getClass().getSimpleName()
                )
                .register(meterRegistry)
                .increment();
    }

    private void recordLag(Consumer consumer, String queue, String status, ImMessageDispatchEvent event) {
        if (event == null || event.getSendTime() == null) {
            return;
        }

        long lagMillis = Duration.between(event.getSendTime(), LocalDateTime.now()).toMillis();
        double lagSeconds = Math.max(0, lagMillis) / 1000.0;
        DistributionSummary.builder("im.mq.consumer.lag")
                .description("IM MQ consumer lag from event sendTime to consume completion")
                .baseUnit("seconds")
                .tags(
                        "consumer", consumer.tag,
                        "queue", queue,
                        "status", status
                )
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(lagSeconds);
    }

    private String queueName(Consumer consumer) {
        return switch (consumer) {
            case SINGLE_MESSAGE_PERSIST -> imMqProperties.getMessagePersistQueue();
            case SINGLE_CONVERSATION_PERSIST -> imMqProperties.getConversationPersistQueue();
            case SINGLE_CONVERSATION_REDIS_PROJECTION -> imMqProperties.getConversationRedisProjectionQueue();
            case SINGLE_RECENT_MESSAGE_CACHE_PROJECTION -> imMqProperties.getRecentMessageCacheProjectionQueue();
            case SINGLE_REALTIME_PUSH -> imMqProperties.getRealtimePushQueue();
            case GROUP_MESSAGE_PERSIST -> imMqProperties.getGroupMessagePersistQueue();
            case GROUP_CONVERSATION_REDIS_PROJECTION -> imMqProperties.getGroupConversationRedisProjectionQueue();
            case GROUP_RECENT_MESSAGE_CACHE_PROJECTION -> imMqProperties.getGroupRecentMessageCacheProjectionQueue();
            case GROUP_REALTIME_PUSH -> imMqProperties.getGroupRealtimePushQueue();
        };
    }

    public enum Consumer {
        SINGLE_MESSAGE_PERSIST("single_message_persist"),
        SINGLE_CONVERSATION_PERSIST("single_conversation_persist"),
        SINGLE_CONVERSATION_REDIS_PROJECTION("single_conversation_redis_projection"),
        SINGLE_RECENT_MESSAGE_CACHE_PROJECTION("single_recent_message_cache_projection"),
        SINGLE_REALTIME_PUSH("single_realtime_push"),
        GROUP_MESSAGE_PERSIST("group_message_persist"),
        GROUP_CONVERSATION_REDIS_PROJECTION("group_conversation_redis_projection"),
        GROUP_RECENT_MESSAGE_CACHE_PROJECTION("group_recent_message_cache_projection"),
        GROUP_REALTIME_PUSH("group_realtime_push");

        private final String tag;

        Consumer(String tag) {
            this.tag = tag;
        }
    }
}
