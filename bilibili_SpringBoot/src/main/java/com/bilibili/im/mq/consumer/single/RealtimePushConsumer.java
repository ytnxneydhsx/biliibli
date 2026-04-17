package com.bilibili.im.mq.consumer.single;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.app.MessagePushApplicationService;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RealtimePushConsumer {

    private final MessagePushApplicationService messagePushApplicationService;

    public RealtimePushConsumer(MessagePushApplicationService messagePushApplicationService) {
        this.messagePushApplicationService = messagePushApplicationService;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.realtimePushQueue}",
            containerFactory = "imRealtimePushListenerContainerFactory"
    )
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            if (event == null) {
                throw new IllegalArgumentException("event is invalid");
            }

            messagePushApplicationService.pushMessageToSender(event);
            messagePushApplicationService.pushMessageToReceiver(event);
        }
    }
}
