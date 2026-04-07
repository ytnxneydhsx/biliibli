package com.bilibili.im.app;

import com.bilibili.im.conversation.model.vo.GroupConversationWindowListVO;

import java.time.LocalDateTime;

public interface GroupConversationWindowApplicationService {

    GroupConversationWindowListVO listRecentGroupConversations(Long ownerUserId);

    void projectGroupConversationCardToRedis(Long groupId,
                                             String lastMessage,
                                             LocalDateTime lastMessageTime,
                                             Long lastServerMessageId);
}
