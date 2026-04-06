package com.bilibili.im.message.service.support;

import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.model.vo.MessageHistoryVO;
import com.bilibili.im.message.model.vo.MessageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MessageQuerySupport {

    private final ObjectMapper objectMapper;

    public MessageQuerySupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MessageVO toMessageVO(ChatMessageDO message) {
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

    public MessageHistoryVO buildInitialHistory(List<MessageVO> records, int cacheLimit) {
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

    public MessageHistoryVO buildPagedHistory(List<ChatMessageDO> queried, int pageSize) {
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

    public String buildSingleConversationId(Long firstUserId, Long secondUserId) {
        BigInteger left = BigInteger.valueOf(firstUserId);
        BigInteger right = BigInteger.valueOf(secondUserId);
        return left.compareTo(right) <= 0
                ? "single_%d_%d".formatted(firstUserId, secondUserId)
                : "single_%d_%d".formatted(secondUserId, firstUserId);
    }

    public String buildGroupConversationId(Long groupId) {
        return "g_%d".formatted(groupId);
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
}
