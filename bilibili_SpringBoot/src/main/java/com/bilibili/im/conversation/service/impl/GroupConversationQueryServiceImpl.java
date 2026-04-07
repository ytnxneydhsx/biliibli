package com.bilibili.im.conversation.service.impl;

import com.bilibili.im.conversation.cache.group.GroupConversationCacheTuning;
import com.bilibili.im.conversation.cache.group.GroupConversationCardCacheService;
import com.bilibili.im.conversation.cache.group.model.GroupConversationCardCacheValue;
import com.bilibili.im.conversation.model.entity.ChatGroupConversationDO;
import com.bilibili.im.conversation.model.vo.GroupConversationWindowListVO;
import com.bilibili.im.conversation.model.vo.GroupConversationWindowVO;
import com.bilibili.im.conversation.service.ChatGroupConversationService;
import com.bilibili.im.conversation.service.GroupConversationQueryService;
import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GroupConversationQueryServiceImpl implements GroupConversationQueryService {

    private final ChatGroupConversationService chatGroupConversationService;
    private final GroupConversationCardCacheService groupConversationCardCacheService;
    private final ChatGroupMapper chatGroupMapper;

    public GroupConversationQueryServiceImpl(ChatGroupConversationService chatGroupConversationService,
                                             GroupConversationCardCacheService groupConversationCardCacheService,
                                             ChatGroupMapper chatGroupMapper) {
        this.chatGroupConversationService = chatGroupConversationService;
        this.groupConversationCardCacheService = groupConversationCardCacheService;
        this.chatGroupMapper = chatGroupMapper;
    }

    @Override
    public GroupConversationWindowListVO listRecentGroupConversations(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }

        List<ChatGroupConversationDO> conversations = chatGroupConversationService.listVisibleGroupConversationStates(ownerUserId);
        if (conversations == null || conversations.isEmpty()) {
            return buildResult(ownerUserId, List.of());
        }

        List<Long> groupIds = conversations.stream()
                .map(ChatGroupConversationDO::getGroupId)
                .toList();
        Map<Long, GroupConversationCardCacheValue> cards = resolveGroupCards(groupIds);

        List<GroupConversationWindowVO> records = new ArrayList<>(conversations.size());
        for (ChatGroupConversationDO conversation : conversations) {
            GroupConversationCardCacheValue card = cards.get(conversation.getGroupId());
            if (card == null) {
                continue;
            }
            records.add(toWindowVO(conversation, card));
        }
        records.sort(Comparator
                .comparing(GroupConversationWindowVO::getSortTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(GroupConversationWindowVO::getGroupId, Comparator.nullsLast(Comparator.reverseOrder())));

        return buildResult(ownerUserId, records);
    }

    private GroupConversationWindowListVO buildResult(Long ownerUserId, List<GroupConversationWindowVO> records) {
        GroupConversationWindowListVO result = new GroupConversationWindowListVO();
        result.setOwnerUserId(ownerUserId);
        result.setSize(records == null ? 0 : records.size());
        result.setRecords(records);
        return result;
    }

    private Map<Long, GroupConversationCardCacheValue> resolveGroupCards(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, GroupConversationCardCacheValue> cachedCards = groupConversationCardCacheService.getGroupCards(groupIds);
        Set<Long> uniqueGroupIds = new LinkedHashSet<>(groupIds);
        if (cachedCards.size() >= uniqueGroupIds.size()) {
            return cachedCards;
        }

        List<Long> missedGroupIds = uniqueGroupIds.stream()
                .filter(groupId -> !cachedCards.containsKey(groupId))
                .toList();
        if (missedGroupIds.isEmpty()) {
            return cachedCards;
        }

        List<ChatGroupDO> groups = chatGroupMapper.selectByIds(missedGroupIds);
        if (groups != null && !groups.isEmpty()) {
            List<GroupConversationCardCacheValue> missedCards = groups.stream()
                    .map(this::toCardCacheValue)
                    .toList();
            Map<Long, GroupConversationCardCacheValue> merged = new java.util.HashMap<>(cachedCards);
            for (GroupConversationCardCacheValue missedCard : missedCards) {
                merged.put(missedCard.getGroupId(), missedCard);
            }
            return merged;
        }
        return cachedCards;
    }

    private GroupConversationWindowVO toWindowVO(ChatGroupConversationDO conversation,
                                                 GroupConversationCardCacheValue card) {
        GroupConversationWindowVO window = new GroupConversationWindowVO();
        window.setConversationId(conversation.getConversationId());
        window.setGroupId(conversation.getGroupId());
        window.setGroupName(card.getGroupName());
        window.setGroupAvatar(card.getGroupAvatar());
        window.setStatus(card.getStatus());
        window.setMemberCount(card.getMemberCount());
        window.setIsAllMuted(card.getIsAllMuted());
        window.setLastMessage(card.getLastMessage());
        window.setLastMessageTime(card.getLastMessageTime());
        window.setLastServerMessageId(card.getLastServerMessageId());
        window.setLastMessageSeq(card.getLastMessageSeq());
        window.setLastReadSeq(conversation.getLastReadSeq());
        window.setUnreadCount(resolveUnreadCount(card.getLastMessageSeq(), conversation.getLastReadSeq()));
        window.setIsMuted(conversation.getIsMuted());
        window.setSortTime(resolveSortTime(card.getLastMessageTime(), conversation.getUpdateTime()));
        return window;
    }

    private GroupConversationCardCacheValue toCardCacheValue(ChatGroupDO group) {
        GroupConversationCardCacheValue value = new GroupConversationCardCacheValue();
        value.setGroupId(group.getId());
        value.setGroupName(group.getGroupName());
        value.setGroupAvatar(group.getGroupAvatar());
        value.setStatus(group.getStatus());
        value.setMemberCount(group.getMemberCount());
        value.setIsAllMuted(group.getIsAllMuted());
        value.setLastMessage(group.getLastMessage());
        value.setLastMessageTime(group.getLastMessageTime());
        value.setLastServerMessageId(group.getLastServerMessageId());
        value.setLastMessageSeq(group.getLastMessageSeq());
        return value;
    }

    private LocalDateTime resolveSortTime(LocalDateTime lastMessageTime, LocalDateTime conversationUpdateTime) {
        return lastMessageTime != null ? lastMessageTime : conversationUpdateTime;
    }

    private Integer resolveUnreadCount(Long lastMessageSeq, Long lastReadSeq) {
        long unread = Math.max(0L, safeLong(lastMessageSeq) - safeLong(lastReadSeq));
        if (unread > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) unread;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
