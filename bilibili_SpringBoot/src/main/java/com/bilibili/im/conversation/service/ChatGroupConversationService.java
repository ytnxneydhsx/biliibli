package com.bilibili.im.conversation.service;

import com.bilibili.im.conversation.model.vo.GroupConversationWindowVO;

import java.util.List;

public interface ChatGroupConversationService {

    String resolveGroupConversationId(Long groupId);

    void initializeGroupConversation(Long ownerUserId, Long groupId, Long lastReadSeq);

    void hideGroupConversation(Long ownerUserId, Long groupId);

    void hideAllGroupConversations(Long groupId);

    List<GroupConversationWindowVO> listVisibleGroupConversations(Long ownerUserId);
}
