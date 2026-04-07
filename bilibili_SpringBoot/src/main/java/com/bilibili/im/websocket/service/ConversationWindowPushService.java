package com.bilibili.im.websocket.service;

import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.model.dto.GroupConversationWindowUpdateDTO;

public interface ConversationWindowPushService {

    void pushSingleConversationUpdated(Long ownerUserId, ConversationWindowUpdateDTO update);

    void pushGroupConversationUpdated(Long ownerUserId, GroupConversationWindowUpdateDTO update);
}
