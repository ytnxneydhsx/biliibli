package com.bilibili.im.app;

import com.bilibili.im.conversation.model.vo.ConversationWindowListVO;

import java.time.LocalDateTime;

public interface ConversationWindowApplicationService {

    ConversationWindowListVO listRecentConversations(Long ownerUserId);

    void projectSingleMessageToConversationWindows(String conversationId,
                                                   Long senderId,
                                                   Long receiverId,
                                                   String lastMessage,
                                                   LocalDateTime lastMessageTime,
                                                   Long lastServerMessageId);

    void projectSingleMessageToRedisConversationWindows(String conversationId,
                                                        Long senderId,
                                                        Long receiverId,
                                                        String lastMessage,
                                                        LocalDateTime lastMessageTime,
                                                        Long lastServerMessageId);

    void pushUpdatedSingleConversationWindows(Long senderId, Long receiverId);

    void clearSingleConversationUnread(Long ownerUserId, Long peerUserId);
}
