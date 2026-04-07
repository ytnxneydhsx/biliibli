package com.bilibili.im.app.impl;

import com.bilibili.im.app.GroupConversationWindowApplicationService;
import com.bilibili.im.conversation.cache.group.GroupConversationCardCacheService;
import com.bilibili.im.conversation.cache.group.model.GroupConversationCardCacheValue;
import com.bilibili.im.conversation.model.vo.GroupConversationWindowListVO;
import com.bilibili.im.conversation.service.GroupConversationQueryService;
import com.bilibili.im.group.mapper.ChatGroupMemberMapper;
import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberStatus;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.websocket.model.dto.GroupConversationWindowUpdateDTO;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupConversationWindowApplicationServiceImpl implements GroupConversationWindowApplicationService {

    private final GroupConversationQueryService groupConversationQueryService;
    private final GroupConversationCardCacheService groupConversationCardCacheService;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ConversationWindowPushService conversationWindowPushService;

    public GroupConversationWindowApplicationServiceImpl(GroupConversationQueryService groupConversationQueryService,
                                                         GroupConversationCardCacheService groupConversationCardCacheService,
                                                         ChatGroupMemberMapper chatGroupMemberMapper,
                                                         ChatGroupMapper chatGroupMapper,
                                                         ConversationWindowPushService conversationWindowPushService) {
        this.groupConversationQueryService = groupConversationQueryService;
        this.groupConversationCardCacheService = groupConversationCardCacheService;
        this.chatGroupMemberMapper = chatGroupMemberMapper;
        this.chatGroupMapper = chatGroupMapper;
        this.conversationWindowPushService = conversationWindowPushService;
    }

    @Override
    public GroupConversationWindowListVO listRecentGroupConversations(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        return groupConversationQueryService.listRecentGroupConversations(ownerUserId);
    }

    @Override
    public void projectGroupConversationCardToRedis(Long groupId,
                                                    String lastMessage,
                                                    LocalDateTime lastMessageTime,
                                                    Long lastServerMessageId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        GroupConversationCardCacheValue cachedCard = groupConversationCardCacheService.getGroupCard(groupId);
        if (cachedCard != null) {
            if (!shouldApplyGroupCardEvent(cachedCard.getLastServerMessageId(), lastServerMessageId)) {
                return;
            }

            GroupConversationCardCacheValue next = copyGroupCard(cachedCard);
            next.setLastMessage(lastMessage);
            next.setLastMessageTime(lastMessageTime);
            next.setLastServerMessageId(lastServerMessageId);
            next.setLastMessageSeq(resolveNextGroupMessageSeq(cachedCard.getLastMessageSeq()));
            groupConversationCardCacheService.cacheGroupCard(next);
            pushGroupConversationChangedPlaceholder(groupId, lastServerMessageId);
            return;
        }

        ChatGroupDO persistedGroup = chatGroupMapper.selectById(groupId);
        if (persistedGroup == null) {
            return;
        }
        if (!isSameOrNewerGroupCardVersion(persistedGroup.getLastServerMessageId(), lastServerMessageId)) {
            return;
        }

        groupConversationCardCacheService.cacheGroupCard(toGroupConversationCardCacheValue(persistedGroup));
        pushGroupConversationChangedPlaceholder(groupId, lastServerMessageId);
    }

    private boolean shouldApplyGroupCardEvent(Long baselineServerMessageId, Long eventServerMessageId) {
        if (eventServerMessageId == null) {
            return true;
        }
        if (baselineServerMessageId == null) {
            return true;
        }
        return baselineServerMessageId < eventServerMessageId;
    }

    private boolean isSameOrNewerGroupCardVersion(Long baselineServerMessageId, Long eventServerMessageId) {
        if (baselineServerMessageId == null || eventServerMessageId == null) {
            return false;
        }
        return baselineServerMessageId >= eventServerMessageId;
    }

    private Long resolveNextGroupMessageSeq(Long currentSeq) {
        return currentSeq == null ? null : currentSeq + 1L;
    }

    private GroupConversationCardCacheValue toGroupConversationCardCacheValue(ChatGroupDO group) {
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

    private GroupConversationCardCacheValue copyGroupCard(GroupConversationCardCacheValue card) {
        GroupConversationCardCacheValue copy = new GroupConversationCardCacheValue();
        copy.setGroupId(card.getGroupId());
        copy.setGroupName(card.getGroupName());
        copy.setGroupAvatar(card.getGroupAvatar());
        copy.setStatus(card.getStatus());
        copy.setMemberCount(card.getMemberCount());
        copy.setIsAllMuted(card.getIsAllMuted());
        copy.setLastMessage(card.getLastMessage());
        copy.setLastMessageTime(card.getLastMessageTime());
        copy.setLastServerMessageId(card.getLastServerMessageId());
        copy.setLastMessageSeq(card.getLastMessageSeq());
        return copy;
    }

    private void pushGroupConversationChangedPlaceholder(Long groupId, Long lastServerMessageId) {
        if (groupId == null || groupId <= 0) {
            return;
        }

        List<ChatGroupMemberDO> activeMembers = chatGroupMemberMapper.selectByGroupIdAndStatus(
                groupId,
                ChatGroupMemberStatus.ACTIVE.getCode()
        );
        if (activeMembers == null || activeMembers.isEmpty()) {
            return;
        }

        GroupConversationWindowUpdateDTO update = new GroupConversationWindowUpdateDTO();
        update.setGroupId(groupId);
        update.setLastServerMessageId(lastServerMessageId);
        for (ChatGroupMemberDO member : activeMembers) {
            if (member == null || member.getUserId() == null || member.getUserId() <= 0) {
                continue;
            }
            conversationWindowPushService.pushGroupConversationUpdated(member.getUserId(), update);
        }
    }
}
