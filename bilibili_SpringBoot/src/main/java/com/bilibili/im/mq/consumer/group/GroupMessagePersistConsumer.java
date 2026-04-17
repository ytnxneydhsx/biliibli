package com.bilibili.im.mq.consumer.group;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.service.GroupMessagePersistService;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.metrics.ImMqConsumerMetrics;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bilibili.im.mq.metrics.ImMqConsumerMetrics.Consumer.GROUP_MESSAGE_PERSIST;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class GroupMessagePersistConsumer {

    private final GroupMessagePersistService groupMessagePersistService;
    private final ImMqConsumerMetrics imMqConsumerMetrics;

    public GroupMessagePersistConsumer(GroupMessagePersistService groupMessagePersistService,
                                       ImMqConsumerMetrics imMqConsumerMetrics) {
        this.groupMessagePersistService = groupMessagePersistService;
        this.imMqConsumerMetrics = imMqConsumerMetrics;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.groupMessagePersistQueue}",
            containerFactory = "imGroupPersistListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            imMqConsumerMetrics.record(GROUP_MESSAGE_PERSIST, event, () -> {
                if (event == null) {
                    throw new IllegalArgumentException("event is invalid");
                }
                if (!Integer.valueOf(ConversationType.GROUP.getCode()).equals(event.getConversationType())) {
                    throw new IllegalArgumentException("conversationType is invalid");
                }
                groupMessagePersistService.persistGroupMessage(event.getReceiverId(), buildPersistMessageCommand(event));
            });
        }
    }

    private static PersistMessageCommand buildPersistMessageCommand(ImMessageDispatchEvent event) {
        PersistMessageCommand command = new PersistMessageCommand();
        command.setServerMessageId(event.getServerMessageId());
        command.setConversationId(event.getConversationId());
        command.setConversationType(event.getConversationType());
        command.setSenderId(event.getSenderId());
        command.setReceiverId(event.getReceiverId());
        command.setClientMessageId(event.getClientMessageId());
        command.setSenderLocation(event.getSenderLocation());
        command.setMessageType(event.getMessageType());
        command.setContent(event.getContent());
        command.setSendTime(event.getSendTime());
        return command;
    }
}
