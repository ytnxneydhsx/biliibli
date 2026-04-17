package com.bilibili.im.websocket.service.impl;

import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.dto.GroupConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import com.bilibili.im.websocket.service.ImWebSocketOutboundSender;
import org.springframework.stereotype.Service;

@Service
public class ConversationWindowPushServiceImpl implements ConversationWindowPushService {

    private final ImWebSocketOutboundSender outboundSender;

    public ConversationWindowPushServiceImpl(ImWebSocketOutboundSender outboundSender) {
        this.outboundSender = outboundSender;
    }

    @Override
    public void pushSingleConversationUpdated(Long ownerUserId, ConversationWindowUpdateDTO update) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (update == null) {
            return;
        }
        outboundSender.sendToUser(ownerUserId, ImWebSocketMessageType.CONVERSATION_UPDATED, update);
    }

    @Override
    public void pushGroupConversationUpdated(Long ownerUserId, GroupConversationWindowUpdateDTO update) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (update == null) {
            return;
        }
        outboundSender.sendToUser(ownerUserId, ImWebSocketMessageType.GROUP_CONVERSATION_UPDATED, update);
    }
}
