package com.bilibili.im.message.service.impl;

import com.bilibili.im.message.mapper.ChatMessageMapper;
import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.model.enum.MessageStatus;
import com.bilibili.im.message.model.enum.MessageType;
import com.bilibili.im.message.service.ChatMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper,
                                  ObjectMapper objectMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatMessageDO persistMessage(PersistMessageCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is invalid");
        }
        if (command.getConversationId() == null || command.getConversationId().isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }
        if (command.getSenderId() == null || command.getSenderId() <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (command.getReceiverId() == null || command.getReceiverId() <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        if (command.getClientMessageId() == null || command.getClientMessageId() <= 0) {
            throw new IllegalArgumentException("clientMessageId is invalid");
        }
        if (command.getSendTime() == null) {
            throw new IllegalArgumentException("sendTime is invalid");
        }
        if (!MessageType.supports(command.getMessageType())) {
            throw new IllegalArgumentException("messageType is invalid");
        }
        if (command.getContent() == null) {
            throw new IllegalArgumentException("content is invalid");
        }

        ChatMessageDO message = new ChatMessageDO();
        message.setConversationId(command.getConversationId());
        message.setSenderId(command.getSenderId());
        message.setReceiverId(command.getReceiverId());
        message.setClientMessageId(command.getClientMessageId());
        message.setMessageType(command.getMessageType());
        message.setContent(serializeContent(command));
        message.setSendTime(command.getSendTime());
        message.setStatus(MessageStatus.SUCCESS.getCode());

        try {
            int rows = chatMessageMapper.insert(message);
            if (rows != 1 || message.getId() == null) {
                throw new RuntimeException("insert chat message failed");
            }
            return message;
        } catch (DuplicateKeyException ex) {
            ChatMessageDO existingMessage = chatMessageMapper.selectBySenderAndClientMessageId(
                    command.getSenderId(),
                    command.getClientMessageId()
            );
            if (existingMessage == null || existingMessage.getId() == null) {
                throw new RuntimeException("duplicate chat message detected but existing message not found", ex);
            }
            return existingMessage;
        }
    }

    private String serializeContent(PersistMessageCommand command) {
        try {
            return objectMapper.writeValueAsString(command.getContent());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("serialize message content failed", ex);
        }
    }
}
