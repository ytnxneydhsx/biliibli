package com.bilibili.im.websocket.service;

import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;

public interface ImWebSocketOutboundSender {

    void sendToUser(Long userId, ImWebSocketMessageType messageType, Object data);
}
