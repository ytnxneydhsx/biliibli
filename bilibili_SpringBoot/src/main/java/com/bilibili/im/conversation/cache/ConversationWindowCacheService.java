package com.bilibili.im.conversation.cache;

import com.bilibili.im.conversation.cache.model.ConversationWindowCacheValue;
import com.bilibili.im.conversation.model.vo.ConversationWindowVO;

import java.util.List;

public interface ConversationWindowCacheService {

    boolean isInitialized(Long ownerUserId);

    List<ConversationWindowVO> listRecentConversations(Long ownerUserId);

    void replaceRecentConversations(Long ownerUserId, List<ConversationWindowVO> records);

    void cacheConversationWindow(Long ownerUserId, ConversationWindowVO window);

    ConversationWindowCacheValue getConversationWindow(Long ownerUserId, String conversationId);

    void cacheConversationWindowValue(Long ownerUserId, ConversationWindowCacheValue window);
}
