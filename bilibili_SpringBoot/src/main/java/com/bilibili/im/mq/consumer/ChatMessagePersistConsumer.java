package com.bilibili.im.mq.consumer;

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

    public ChatMessagePersistConsumer(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @RabbitListener(queues = "#{@imMqProperties.messagePersistQueue}")
    @Transactional(rollbackFor = Exception.class)
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        chatMessageService.persistMessage(buildPersistMessageCommand(event));
    }

    private static PersistMessageCommand buildPersistMessageCommand(ImMessageDispatchEvent event) {
        PersistMessageCommand command = new PersistMessageCommand();
        command.setConversationId(event.getConversationId());
        command.setSenderId(event.getSenderId());
        command.setReceiverId(event.getReceiverId());
        command.setClientMessageId(event.getClientMessageId());
        command.setMessageType(event.getMessageType());
        command.setContent(event.getContent());
        command.setSendTime(event.getSendTime());
        return command;
    }
}
