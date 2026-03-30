package com.bilibili.im.websocket.service;

import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;

public interface ConversationWindowPushService {

    void pushSingleConversationUpdated(Long ownerUserId, ConversationWindowUpdateDTO update);
}
