package com.bilibili.im.conversation.service;

public interface ChatGroupConversationService {

    String resolveGroupConversationId(Long groupId);

    void initializeGroupConversation(Long ownerUserId, Long groupId);
}
