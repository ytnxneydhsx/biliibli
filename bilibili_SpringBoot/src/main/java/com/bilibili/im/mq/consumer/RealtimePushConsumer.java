package com.bilibili.im.mq.consumer;

import com.bilibili.im.app.MessagePushApplicationService;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RealtimePushConsumer {

    private final MessagePushApplicationService messagePushApplicationService;

    public RealtimePushConsumer(MessagePushApplicationService messagePushApplicationService) {
        this.messagePushApplicationService = messagePushApplicationService;
    }

    @RabbitListener(queues = "#{@imMqProperties.realtimePushQueue}")
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }

        messagePushApplicationService.pushMessageToSender(event);
        messagePushApplicationService.pushMessageToReceiver(event);
    }
}
