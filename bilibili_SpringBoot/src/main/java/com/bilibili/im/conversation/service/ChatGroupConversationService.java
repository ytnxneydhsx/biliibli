package com.bilibili.im.conversation.service;

import com.bilibili.im.conversation.model.vo.GroupConversationWindowVO;
import com.bilibili.im.conversation.model.entity.ChatGroupConversationDO;

import java.util.List;

public interface ChatGroupConversationService {

    String resolveGroupConversationId(Long groupId);

    void initializeGroupConversation(Long ownerUserId, Long groupId, Long lastReadSeq);

    void hideGroupConversation(Long ownerUserId, Long groupId);

    void hideAllGroupConversations(Long groupId);

    void advanceLastReadSeq(Long ownerUserId, Long groupId);

    List<GroupConversationWindowVO> listVisibleGroupConversations(Long ownerUserId);

    List<ChatGroupConversationDO> listVisibleGroupConversationStates(Long ownerUserId);
}
