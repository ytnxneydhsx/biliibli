package com.bilibili.im.message.service.impl;

import com.bilibili.im.message.cache.MessageCacheTuning;
import com.bilibili.im.message.cache.RecentMessageCacheService;
import com.bilibili.im.message.mapper.ChatMessageMapper;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.model.vo.MessageHistoryVO;
import com.bilibili.im.message.model.vo.MessageVO;
import com.bilibili.im.message.service.SingleMessageQueryService;
import com.bilibili.im.message.service.support.MessageQuerySupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SingleMessageQueryServiceImpl implements SingleMessageQueryService {

    private final ChatMessageMapper chatMessageMapper;
    private final RecentMessageCacheService recentMessageCacheService;
    private final MessageQuerySupport messageQuerySupport;

    public SingleMessageQueryServiceImpl(ChatMessageMapper chatMessageMapper,
                                         RecentMessageCacheService recentMessageCacheService,
                                         MessageQuerySupport messageQuerySupport) {
        this.chatMessageMapper = chatMessageMapper;
        this.recentMessageCacheService = recentMessageCacheService;
        this.messageQuerySupport = messageQuerySupport;
    }

    @Override
    public MessageHistoryVO querySingleMessageHistory(Long ownerUserId, Long peerUserId, Long beforeServerMessageId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (peerUserId == null || peerUserId <= 0) {
            throw new IllegalArgumentException("peerUserId is invalid");
        }
        if (beforeServerMessageId != null && beforeServerMessageId <= 0) {
            throw new IllegalArgumentException("beforeServerMessageId is invalid");
        }

        String conversationId = messageQuerySupport.buildSingleConversationId(ownerUserId, peerUserId);
        if (beforeServerMessageId == null) {
            return queryRecentMessageHistoryWithCache(conversationId);
        }

        return queryOlderMessageHistory(conversationId, beforeServerMessageId);
    }

    private MessageHistoryVO queryRecentMessageHistoryWithCache(String conversationId) {
        int cacheLimit = MessageCacheTuning.RECENT_MESSAGE_CACHE_LIMIT;
        List<MessageVO> cachedRecords = recentMessageCacheService.listRecentMessages(conversationId, cacheLimit);
        if (cachedRecords != null) {
            return messageQuerySupport.buildInitialHistory(cachedRecords, cacheLimit);
        }

        List<ChatMessageDO> queried = chatMessageMapper.selectHistoryByConversationId(
                conversationId,
                null,
                cacheLimit
        );
        if (queried == null || queried.isEmpty()) {
            recentMessageCacheService.initializeRecentMessages(conversationId, Collections.emptyList());
            return messageQuerySupport.buildInitialHistory(Collections.emptyList(), cacheLimit);
        }

        List<ChatMessageDO> initRecords = new ArrayList<>(queried);
        Collections.reverse(initRecords);

        List<MessageVO> cacheSource = initRecords.stream()
                .map(messageQuerySupport::toMessageVO)
                .toList();
        recentMessageCacheService.initializeRecentMessages(conversationId, cacheSource);

        List<MessageVO> pageRecords = recentMessageCacheService.listRecentMessages(conversationId, cacheLimit);
        if (pageRecords == null) {
            pageRecords = cacheSource;
        }
        return messageQuerySupport.buildInitialHistory(pageRecords, cacheLimit);
    }

    private MessageHistoryVO queryOlderMessageHistory(String conversationId, Long beforeServerMessageId) {
        int pageSize = MessageCacheTuning.HISTORY_PAGE_SIZE;
        List<ChatMessageDO> queried = chatMessageMapper.selectHistoryByConversationId(
                conversationId,
                beforeServerMessageId,
                pageSize + 1
        );
        return messageQuerySupport.buildPagedHistory(queried, pageSize);
    }
}
