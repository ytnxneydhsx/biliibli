package com.bilibili.im.conversation.cache.group;

import com.bilibili.im.conversation.cache.group.model.GroupConversationCardCacheValue;
import java.util.List;
import java.util.Map;

public interface GroupConversationCardCacheService {

    Map<Long, GroupConversationCardCacheValue> getGroupCards(List<Long> groupIds);

    void cacheGroupCards(List<GroupConversationCardCacheValue> records);
}
