package com.bilibili.im.message.service.impl;

import com.bilibili.im.message.cache.RecentMessageCacheService;
import com.bilibili.im.message.cache.MessageCacheTuning;
import com.bilibili.im.message.mapper.ChatMessageMapper;
import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.model.enums.MessageStatus;
import com.bilibili.im.message.model.enums.MessageType;
import com.bilibili.im.message.model.vo.MessageHistoryVO;
import com.bilibili.im.message.model.vo.MessageVO;
import com.bilibili.im.message.service.ChatMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final RecentMessageCacheService recentMessageCacheService;
    private final ObjectMapper objectMapper;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper,
                                  RecentMessageCacheService recentMessageCacheService,
                                  ObjectMapper objectMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.recentMessageCacheService = recentMessageCacheService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatMessageDO persistMessage(PersistMessageCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is invalid");
        }
        if (command.getServerMessageId() == null || command.getServerMessageId() <= 0) {
            throw new IllegalArgumentException("serverMessageId is invalid");
        }
        if (command.getConversationId() == null || command.getConversationId().isBlank()) {
            throw new IllegalArgumentException("conversationId is invalid");
        }
        if (command.getSenderId() == null || command.getSenderId() <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (command.getReceiverId() == null || command.getReceiverId() <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        if (command.getClientMessageId() == null || command.getClientMessageId() <= 0) {
            throw new IllegalArgumentException("clientMessageId is invalid");
        }
        if (command.getSendTime() == null) {
            throw new IllegalArgumentException("sendTime is invalid");
        }
        if (!MessageType.supports(command.getMessageType())) {
            throw new IllegalArgumentException("messageType is invalid");
        }
        if (command.getContent() == null) {
            throw new IllegalArgumentException("content is invalid");
        }

        ChatMessageDO message = new ChatMessageDO();
        message.setServerMessageId(command.getServerMessageId());
        message.setConversationId(command.getConversationId());
        message.setSenderId(command.getSenderId());
        message.setReceiverId(command.getReceiverId());
        message.setClientMessageId(command.getClientMessageId());
        message.setSenderLocation(command.getSenderLocation());
        message.setMessageType(command.getMessageType());
        message.setContent(serializeContent(command));
        message.setSendTime(command.getSendTime());
        message.setStatus(MessageStatus.SUCCESS.getCode());

        try {
            int rows = chatMessageMapper.insert(message);
            if (rows != 1 || message.getId() == null) {
                throw new RuntimeException("insert chat message failed");
            }
            return message;
        } catch (DuplicateKeyException ex) {
            ChatMessageDO existingMessage = chatMessageMapper.selectBySenderAndClientMessageId(
                    command.getSenderId(),
                    command.getClientMessageId()
            );
            if (existingMessage == null || existingMessage.getId() == null) {
                throw new RuntimeException("duplicate chat message detected but existing message not found", ex);
            }
            return existingMessage;
        }
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

        String conversationId = buildSingleConversationId(ownerUserId, peerUserId);
        if (beforeServerMessageId == null) {
            return queryRecentMessageHistoryWithCache(conversationId);
        }

        return queryOlderMessageHistory(conversationId, beforeServerMessageId);
    }

    private MessageHistoryVO queryRecentMessageHistoryWithCache(String conversationId) {
        int cacheLimit = MessageCacheTuning.RECENT_MESSAGE_CACHE_LIMIT;
        List<MessageVO> cachedRecords = recentMessageCacheService.listRecentMessages(conversationId, cacheLimit);
        if (cachedRecords != null) {
            return buildInitialHistory(cachedRecords, cacheLimit);
        }

        List<ChatMessageDO> queried = chatMessageMapper.selectHistoryByConversationId(
                conversationId,
                null,
                cacheLimit
        );
        if (queried == null || queried.isEmpty()) {
            recentMessageCacheService.initializeRecentMessages(conversationId, Collections.emptyList());
            MessageHistoryVO empty = new MessageHistoryVO();
            empty.setRecords(Collections.emptyList());
            empty.setHasMore(false);
            empty.setNextBeforeServerMessageId(null);
            return empty;
        }

        List<ChatMessageDO> initRecords = new ArrayList<>(queried);
        Collections.reverse(initRecords);

        List<MessageVO> cacheSource = initRecords.stream()
                .map(this::toMessageVO)
                .toList();
        recentMessageCacheService.initializeRecentMessages(conversationId, cacheSource);

        List<MessageVO> pageRecords = recentMessageCacheService.listRecentMessages(conversationId, cacheLimit);
        if (pageRecords == null) {
            pageRecords = cacheSource;
        }
        return buildInitialHistory(pageRecords, cacheLimit);
    }

    private MessageHistoryVO queryOlderMessageHistory(String conversationId, Long beforeServerMessageId) {
        int pageSize = MessageCacheTuning.HISTORY_PAGE_SIZE;
        List<ChatMessageDO> queried = chatMessageMapper.selectHistoryByConversationId(
                conversationId,
                beforeServerMessageId,
                pageSize + 1
        );
        if (queried == null || queried.isEmpty()) {
            MessageHistoryVO empty = new MessageHistoryVO();
            empty.setRecords(Collections.emptyList());
            empty.setHasMore(false);
            empty.setNextBeforeServerMessageId(null);
            return empty;
        }

        boolean hasMore = queried.size() > pageSize;
        List<ChatMessageDO> pageRecords = hasMore
                ? new ArrayList<>(queried.subList(0, pageSize))
                : new ArrayList<>(queried);

        Long nextBeforeServerMessageId = hasMore ? pageRecords.get(pageRecords.size() - 1).getServerMessageId() : null;
        Collections.reverse(pageRecords);

        List<MessageVO> records = pageRecords.stream()
                .map(this::toMessageVO)
                .toList();

        MessageHistoryVO history = new MessageHistoryVO();
        history.setRecords(records);
        history.setHasMore(hasMore);
        history.setNextBeforeServerMessageId(nextBeforeServerMessageId);
        return history;
    }

    private MessageHistoryVO buildHistory(List<MessageVO> records, boolean hasMore) {
        MessageHistoryVO history = new MessageHistoryVO();
        history.setRecords(records);
        history.setHasMore(hasMore);
        history.setNextBeforeServerMessageId(
                hasMore && records != null && !records.isEmpty() ? records.get(0).getServerMessageId() : null
        );
        return history;
    }

    private MessageHistoryVO buildInitialHistory(List<MessageVO> records, int cacheLimit) {
        if (records == null || records.isEmpty()) {
            MessageHistoryVO empty = new MessageHistoryVO();
            empty.setRecords(Collections.emptyList());
            empty.setHasMore(false);
            empty.setNextBeforeServerMessageId(null);
            return empty;
        }
        boolean hasMore = records.size() >= cacheLimit;
        return buildHistory(records, hasMore);
    }

    private String serializeContent(PersistMessageCommand command) {
        try {
            return objectMapper.writeValueAsString(command.getContent());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("serialize message content failed", ex);
        }
    }

    private MessageVO toMessageVO(ChatMessageDO message) {
        MessageVO item = new MessageVO();
        item.setId(message.getId());
        item.setServerMessageId(message.getServerMessageId());
        item.setConversationId(message.getConversationId());
        item.setSenderId(message.getSenderId());
        item.setReceiverId(message.getReceiverId());
        item.setClientMessageId(message.getClientMessageId());
        item.setSenderLocation(message.getSenderLocation());
        item.setMessageType(message.getMessageType());
        item.setContent(deserializeContent(message.getContent()));
        item.setSendTime(message.getSendTime());
        item.setStatus(message.getStatus());
        return item;
    }

    private MessageContentDTO deserializeContent(String content) {
        if (content == null || content.isBlank()) {
            return new MessageContentDTO();
        }
        try {
            return objectMapper.readValue(content, MessageContentDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("deserialize message content failed", ex);
        }
    }

    private static String buildSingleConversationId(Long firstUserId, Long secondUserId) {
        BigInteger left = BigInteger.valueOf(firstUserId);
        BigInteger right = BigInteger.valueOf(secondUserId);
        return left.compareTo(right) <= 0
                ? "single_%d_%d".formatted(firstUserId, secondUserId)
                : "single_%d_%d".formatted(secondUserId, firstUserId);
    }
}
