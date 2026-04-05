package com.bilibili.im.conversation.service;

import com.bilibili.im.conversation.model.entity.ChatConversationDO;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatConversationService {

    String resolveSingleConversationId(Long ownerUserId, Long peerUserId);

    String resolveGroupConversationId(Long groupId);

    void initializeGroupConversation(Long ownerUserId, Long groupId);

    List<ChatConversationDO> listRecentSingleConversations(Long ownerUserId);

    ChatConversationDO getSingleConversation(Long ownerUserId, Long peerUserId);

    void updateSenderConversationSummary(String conversationId,
                                         Long senderId,
                                         Long receiverId,
                                         String lastMessage,
                                         LocalDateTime lastMessageTime,
                                         Long lastServerMessageId);

    void updateReceiverConversationSummary(String conversationId,
                                           Long senderId,
                                           Long receiverId,
                                           String lastMessage,
                                           LocalDateTime lastMessageTime,
                                           Long lastServerMessageId);

    void projectSingleMessageToConversationSummaries(String conversationId,
                                                     Long senderId,
                                                     Long receiverId,
                                                     String lastMessage,
                                                     LocalDateTime lastMessageTime,
                                                     Long lastServerMessageId);

    void clearSingleConversationUnread(Long ownerUserId, Long peerUserId);
}
