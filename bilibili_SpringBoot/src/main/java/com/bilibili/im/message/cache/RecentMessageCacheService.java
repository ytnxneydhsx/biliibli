package com.bilibili.im.message.cache;

import com.bilibili.im.message.model.vo.MessageVO;

import java.util.List;

public interface RecentMessageCacheService {

    List<MessageVO> listRecentMessages(String conversationId, int limit);

    void initializeRecentMessages(String conversationId, List<MessageVO> records);

    void appendMessageIfInitialized(String conversationId, MessageVO record);
}
