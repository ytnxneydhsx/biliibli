package com.bilibili.im.conversation.service;

import com.bilibili.im.conversation.model.entity.ChatConversationDO;

import java.time.LocalDateTime;

public interface ChatConversationService {

    String resolveSingleConversationId(Long ownerUserId, Long peerUserId);

    ChatConversationDO getSingleConversation(Long ownerUserId, Long peerUserId);

    void updateSenderConversationSummary(String conversationId,
                                         Long senderId,
                                         Long receiverId,
                                         String lastMessage,
                                         LocalDateTime lastMessageTime);

    void updateReceiverConversationSummary(String conversationId,
                                           Long senderId,
                                           Long receiverId,
                                           String lastMessage,
                                           LocalDateTime lastMessageTime);

    void projectSingleMessageToConversationSummaries(String conversationId,
                                                     Long senderId,
                                                     Long receiverId,
                                                     String lastMessage,
                                                     LocalDateTime lastMessageTime);

    void clearSingleConversationUnread(Long ownerUserId, Long peerUserId);
}
