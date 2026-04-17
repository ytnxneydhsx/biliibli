package com.bilibili.im.mq.consumer.single;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.contact.service.ContactRelationCommandService;
import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.service.ChatMessageService;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.metrics.ImMqConsumerMetrics;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bilibili.im.mq.metrics.ImMqConsumerMetrics.Consumer.SINGLE_MESSAGE_PERSIST;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class ChatMessagePersistConsumer {

    private final ChatMessageService chatMessageService;
    private final ContactRelationCommandService contactRelationCommandService;
    private final ImMqConsumerMetrics imMqConsumerMetrics;

    public ChatMessagePersistConsumer(ChatMessageService chatMessageService,
                                      ContactRelationCommandService contactRelationCommandService,
                                      ImMqConsumerMetrics imMqConsumerMetrics) {
        this.chatMessageService = chatMessageService;
        this.contactRelationCommandService = contactRelationCommandService;
        this.imMqConsumerMetrics = imMqConsumerMetrics;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.messagePersistQueue}",
            containerFactory = "imPersistListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            imMqConsumerMetrics.record(SINGLE_MESSAGE_PERSIST, event, () -> {
                if (event == null) {
                    throw new IllegalArgumentException("event is invalid");
                }
                chatMessageService.persistMessage(buildPersistMessageCommand(event));
                contactRelationCommandService.markDmContact(event.getSenderId(), event.getReceiverId());
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
