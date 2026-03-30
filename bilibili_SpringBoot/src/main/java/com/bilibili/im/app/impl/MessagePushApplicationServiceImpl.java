package com.bilibili.im.app.impl;

import com.bilibili.im.app.MessagePushApplicationService;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.websocket.model.dto.MessagePushDTO;
import com.bilibili.im.websocket.service.MessagePushService;
import org.springframework.stereotype.Service;

@Service
public class MessagePushApplicationServiceImpl implements MessagePushApplicationService {

    private final MessagePushService messagePushService;

    public MessagePushApplicationServiceImpl(MessagePushService messagePushService) {
        this.messagePushService = messagePushService;
    }

    @Override
    public void pushMessageToReceiver(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        messagePushService.pushMessageReceived(event.getReceiverId(), toMessagePush(event));
    }

    private MessagePushDTO toMessagePush(ImMessageDispatchEvent event) {
        MessagePushDTO messagePush = new MessagePushDTO();
        messagePush.setConversationId(event.getConversationId());
        messagePush.setSenderId(event.getSenderId());
        messagePush.setReceiverId(event.getReceiverId());
        messagePush.setClientMessageId(event.getClientMessageId());
        messagePush.setSenderLocation(event.getSenderLocation());
        messagePush.setMessageType(event.getMessageType());
        messagePush.setContent(event.getContent());
        messagePush.setSendTime(event.getSendTime());
        return messagePush;
    }
}
