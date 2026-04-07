package com.bilibili.im.message.service.impl;

import com.bilibili.im.conversation.service.ChatGroupConversationService;
import com.bilibili.im.group.permission.GroupPermissionService;
import com.bilibili.im.message.cache.MessageCacheTuning;
import com.bilibili.im.message.cache.group.GroupRecentMessageCacheService;
import com.bilibili.im.message.mapper.ChatMessageMapper;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.model.vo.MessageHistoryVO;
import com.bilibili.im.message.model.vo.MessageVO;
import com.bilibili.im.message.service.GroupMessageQueryService;
import com.bilibili.im.message.service.support.MessageQuerySupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GroupMessageQueryServiceImpl implements GroupMessageQueryService {

    private final ChatMessageMapper chatMessageMapper;
    private final GroupRecentMessageCacheService groupRecentMessageCacheService;
    private final GroupPermissionService groupPermissionService;
    private final ChatGroupConversationService chatGroupConversationService;
    private final MessageQuerySupport messageQuerySupport;

    public GroupMessageQueryServiceImpl(ChatMessageMapper chatMessageMapper,
                                        GroupRecentMessageCacheService groupRecentMessageCacheService,
                                        GroupPermissionService groupPermissionService,
                                        ChatGroupConversationService chatGroupConversationService,
                                        MessageQuerySupport messageQuerySupport) {
        this.chatMessageMapper = chatMessageMapper;
        this.groupRecentMessageCacheService = groupRecentMessageCacheService;
        this.groupPermissionService = groupPermissionService;
        this.chatGroupConversationService = chatGroupConversationService;
        this.messageQuerySupport = messageQuerySupport;
    }

    @Override
    public MessageHistoryVO queryGroupMessageHistory(Long ownerUserId, Long groupId, Long beforeServerMessageId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (beforeServerMessageId != null && beforeServerMessageId <= 0) {
            throw new IllegalArgumentException("beforeServerMessageId is invalid");
        }

        groupPermissionService.requireActiveGroup(groupId);
        groupPermissionService.requireActiveMembership(groupId, ownerUserId);

        String conversationId = messageQuerySupport.buildGroupConversationId(groupId);
        if (beforeServerMessageId == null) {
            advanceGroupReadSeqToLatest(ownerUserId, groupId);
            return queryRecentGroupMessageHistoryWithCache(groupId, conversationId);
        }

        return queryOlderGroupMessageHistory(groupId, beforeServerMessageId);
    }

    private MessageHistoryVO queryRecentGroupMessageHistoryWithCache(Long groupId, String conversationId) {
        int cacheLimit = MessageCacheTuning.RECENT_MESSAGE_CACHE_LIMIT;
        List<MessageVO> cachedRecords = groupRecentMessageCacheService.listRecentMessages(conversationId, cacheLimit);
        if (cachedRecords != null) {
            return messageQuerySupport.buildInitialHistory(cachedRecords, cacheLimit);
        }

        List<ChatMessageDO> queried = chatMessageMapper.selectGroupHistoryByGroupId(
                groupId,
                null,
                cacheLimit
        );
        if (queried == null || queried.isEmpty()) {
            groupRecentMessageCacheService.initializeRecentMessages(conversationId, Collections.emptyList());
            return messageQuerySupport.buildInitialHistory(Collections.emptyList(), cacheLimit);
        }

        List<ChatMessageDO> initRecords = new ArrayList<>(queried);
        Collections.reverse(initRecords);

        List<MessageVO> cacheSource = initRecords.stream()
                .map(messageQuerySupport::toMessageVO)
                .toList();
        groupRecentMessageCacheService.initializeRecentMessages(conversationId, cacheSource);

        return messageQuerySupport.buildInitialHistory(cacheSource, cacheLimit);
    }

    private MessageHistoryVO queryOlderGroupMessageHistory(Long groupId, Long beforeServerMessageId) {
        int pageSize = MessageCacheTuning.HISTORY_PAGE_SIZE;
        List<ChatMessageDO> queried = chatMessageMapper.selectGroupHistoryByGroupId(
                groupId,
                beforeServerMessageId,
                pageSize + 1
        );
        return messageQuerySupport.buildPagedHistory(queried, pageSize);
    }

    private void advanceGroupReadSeqToLatest(Long ownerUserId, Long groupId) {
        chatGroupConversationService.advanceLastReadSeq(ownerUserId, groupId);
    }
}
