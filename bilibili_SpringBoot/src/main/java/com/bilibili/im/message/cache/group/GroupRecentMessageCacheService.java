package com.bilibili.im.message.cache.group;

import com.bilibili.im.message.model.vo.MessageVO;

import java.util.List;

public interface GroupRecentMessageCacheService {

    List<MessageVO> listRecentMessages(String conversationId, int limit);

    void initializeRecentMessages(String conversationId, List<MessageVO> records);

    void appendMessageIfInitialized(String conversationId, MessageVO record);
}
