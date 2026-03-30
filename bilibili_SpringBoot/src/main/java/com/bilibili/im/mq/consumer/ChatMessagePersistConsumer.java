package com.bilibili.im.mq.consumer;

import com.bilibili.im.contact.service.ContactRelationCommandService;
import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.service.ChatMessageService;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class ChatMessagePersistConsumer {

    private final ChatMessageService chatMessageService;
    private final ContactRelationCommandService contactRelationCommandService;

    public ChatMessagePersistConsumer(ChatMessageService chatMessageService,
                                      ContactRelationCommandService contactRelationCommandService) {
        this.chatMessageService = chatMessageService;
        this.contactRelationCommandService = contactRelationCommandService;
    }

    @RabbitListener(queues = "#{@imMqProperties.messagePersistQueue}")
    @Transactional(rollbackFor = Exception.class)
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        chatMessageService.persistMessage(buildPersistMessageCommand(event));
        contactRelationCommandService.markDmContact(event.getSenderId(), event.getReceiverId());
    }

    private static PersistMessageCommand buildPersistMessageCommand(ImMessageDispatchEvent event) {
        PersistMessageCommand command = new PersistMessageCommand();
        command.setConversationId(event.getConversationId());
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
