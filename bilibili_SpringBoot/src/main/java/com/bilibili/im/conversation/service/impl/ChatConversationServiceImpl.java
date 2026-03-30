package com.bilibili.im.conversation.service.impl;

import com.bilibili.im.conversation.mapper.ChatConversationMapper;
import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.conversation.service.ChatConversationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatConversationServiceImpl implements ChatConversationService {

    private final ChatConversationMapper chatConversationMapper;

    public ChatConversationServiceImpl(ChatConversationMapper chatConversationMapper) {
        this.chatConversationMapper = chatConversationMapper;
    }

    @Override
    public String resolveSingleConversationId(Long ownerUserId, Long peerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (peerUserId == null || peerUserId <= 0) {
            throw new IllegalArgumentException("peerUserId is invalid");
        }

        Integer type = ConversationType.SINGLE.getCode();
        ChatConversationDO ownerConversation = chatConversationMapper.selectByOwnerTargetAndType(ownerUserId, peerUserId, type);
        if (ownerConversation != null && ownerConversation.getConversationId() != null
                && !ownerConversation.getConversationId().isBlank()) {
            return ownerConversation.getConversationId();
        }

        String conversationId = buildSingleConversationId(ownerUserId, peerUserId);

        chatConversationMapper.insertIgnoreConversation(
                conversationId,
                ownerUserId,
                peerUserId,
                type,
                0,
                0
        );
        return conversationId;
    }

    @Override
    public ChatConversationDO getSingleConversation(Long ownerUserId, Long peerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (peerUserId == null || peerUserId <= 0) {
            throw new IllegalArgumentException("peerUserId is invalid");
        }
        return chatConversationMapper.selectByOwnerTargetAndType(
                ownerUserId,
                peerUserId,
                ConversationType.SINGLE.getCode()
        );
    }

    @Override
    public void updateSenderConversationSummary(String conversationId,
                                                Long senderId,
                                                Long receiverId,
                                                String lastMessage,
                                                LocalDateTime lastMessageTime) {
        Integer type = ConversationType.SINGLE.getCode();
        int senderRows = chatConversationMapper.updateSenderConversationSummary(
                conversationId, senderId, receiverId, type, lastMessage, lastMessageTime);
        if (senderRows <= 0) {
            throw new RuntimeException("update sender conversation summary failed");
        }
    }

    @Override
    public void updateReceiverConversationSummary(String conversationId,
                                                  Long senderId,
                                                  Long receiverId,
                                                  String lastMessage,
                                                  LocalDateTime lastMessageTime) {
        String resolvedConversationId = resolveSingleConversationId(receiverId, senderId);
        if (!resolvedConversationId.equals(conversationId)) {
            throw new IllegalStateException("receiver conversation id does not match sender conversation id");
        }

        Integer type = ConversationType.SINGLE.getCode();
        int receiverRows = chatConversationMapper.updateReceiverConversationSummary(
                resolvedConversationId, receiverId, senderId, type, lastMessage, lastMessageTime);
        if (receiverRows <= 0) {
            throw new RuntimeException("update receiver conversation summary failed");
        }
    }

    @Override
    public void projectSingleMessageToConversationSummaries(String conversationId,
                                                            Long senderId,
                                                            Long receiverId,
                                                            String lastMessage,
                                                            LocalDateTime lastMessageTime) {
        updateSenderConversationSummary(conversationId, senderId, receiverId, lastMessage, lastMessageTime);
        updateReceiverConversationSummary(conversationId, senderId, receiverId, lastMessage, lastMessageTime);
    }

    @Override
    public void clearSingleConversationUnread(Long ownerUserId, Long peerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (peerUserId == null || peerUserId <= 0) {
            throw new IllegalArgumentException("peerUserId is invalid");
        }

        chatConversationMapper.resetUnreadCount(
                ownerUserId,
                peerUserId,
                ConversationType.SINGLE.getCode()
        );
    }

    private static String buildSingleConversationId(Long firstUserId, Long secondUserId) {
        long lowUserId = Math.min(firstUserId, secondUserId);
        long highUserId = Math.max(firstUserId, secondUserId);
        return "single_%d_%d".formatted(lowUserId, highUserId);
    }
}
