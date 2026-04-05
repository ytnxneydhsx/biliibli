package com.bilibili.im.conversation.service;

public interface ChatGroupConversationService {

    String resolveGroupConversationId(Long groupId);

    void initializeGroupConversation(Long ownerUserId, Long groupId);

    void hideGroupConversation(Long ownerUserId, Long groupId);

    void hideAllGroupConversations(Long groupId);
}
