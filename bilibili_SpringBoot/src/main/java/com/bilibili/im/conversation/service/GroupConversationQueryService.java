package com.bilibili.im.conversation.service;

import com.bilibili.im.conversation.model.vo.GroupConversationWindowListVO;

public interface GroupConversationQueryService {

    GroupConversationWindowListVO listRecentGroupConversations(Long ownerUserId);
}
