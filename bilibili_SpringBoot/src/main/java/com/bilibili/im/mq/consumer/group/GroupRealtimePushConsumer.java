package com.bilibili.im.mq.consumer.group;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.app.GroupMessagePushApplicationService;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class GroupRealtimePushConsumer {

    private final GroupMessagePushApplicationService groupMessagePushApplicationService;

    public GroupRealtimePushConsumer(GroupMessagePushApplicationService groupMessagePushApplicationService) {
        this.groupMessagePushApplicationService = groupMessagePushApplicationService;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.groupRealtimePushQueue}",
            containerFactory = "imGroupRealtimePushListenerContainerFactory"
    )
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            if (event == null) {
                throw new IllegalArgumentException("event is invalid");
            }
            if (!Integer.valueOf(ConversationType.GROUP.getCode()).equals(event.getConversationType())) {
                return;
            }
            groupMessagePushApplicationService.pushGroupMessage(event);
        }
    }
}
