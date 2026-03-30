package com.bilibili.im.app;

import java.time.LocalDateTime;

public interface ConversationWindowApplicationService {

    void projectSingleMessageToConversationWindows(String conversationId,
                                                   Long senderId,
                                                   Long receiverId,
                                                   String lastMessage,
                                                   LocalDateTime lastMessageTime);

    void pushUpdatedSingleConversationWindows(Long senderId, Long receiverId);

    void clearSingleConversationUnread(Long ownerUserId, Long peerUserId);
}
