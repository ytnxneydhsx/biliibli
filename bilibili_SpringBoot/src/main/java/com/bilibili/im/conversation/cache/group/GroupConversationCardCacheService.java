package com.bilibili.im.conversation.cache.group;

import com.bilibili.im.conversation.cache.group.model.GroupConversationCardCacheValue;
import java.util.List;
import java.util.Map;

public interface GroupConversationCardCacheService {

    GroupConversationCardCacheValue getGroupCard(Long groupId);

    Map<Long, GroupConversationCardCacheValue> getGroupCards(List<Long> groupIds);

    void cacheGroupCard(GroupConversationCardCacheValue record);

    void cacheGroupCards(List<GroupConversationCardCacheValue> records);
}
