package com.bilibili.im.mq.consumer.group;

import com.bilibili.im.app.GroupMessagePushApplicationService;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class GroupRealtimePushConsumer {

    private final GroupMessagePushApplicationService groupMessagePushApplicationService;

    public GroupRealtimePushConsumer(GroupMessagePushApplicationService groupMessagePushApplicationService) {
        this.groupMessagePushApplicationService = groupMessagePushApplicationService;
    }

    @RabbitListener(queues = "#{@imMqProperties.groupRealtimePushQueue}")
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        if (!Integer.valueOf(ConversationType.GROUP.getCode()).equals(event.getConversationType())) {
            return;
        }
        groupMessagePushApplicationService.pushGroupMessage(event);
    }
}
