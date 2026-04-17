package com.bilibili.im.mq.consumer.single;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.app.MessagePushApplicationService;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.metrics.ImMqConsumerMetrics;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static com.bilibili.im.mq.metrics.ImMqConsumerMetrics.Consumer.SINGLE_REALTIME_PUSH;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RealtimePushConsumer {

    private final MessagePushApplicationService messagePushApplicationService;
    private final ImMqConsumerMetrics imMqConsumerMetrics;

    public RealtimePushConsumer(MessagePushApplicationService messagePushApplicationService,
                                ImMqConsumerMetrics imMqConsumerMetrics) {
        this.messagePushApplicationService = messagePushApplicationService;
        this.imMqConsumerMetrics = imMqConsumerMetrics;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.realtimePushQueue}",
            containerFactory = "imRealtimePushListenerContainerFactory"
    )
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            imMqConsumerMetrics.record(SINGLE_REALTIME_PUSH, event, () -> {
                if (event == null) {
                    throw new IllegalArgumentException("event is invalid");
                }

                messagePushApplicationService.pushMessageToSender(event);
                messagePushApplicationService.pushMessageToReceiver(event);
            });
        }
    }
}
