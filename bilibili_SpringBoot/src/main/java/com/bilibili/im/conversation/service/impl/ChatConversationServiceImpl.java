package com.bilibili.im.conversation.service.impl;

import com.bilibili.im.conversation.ConversationWindowTuning;
import com.bilibili.im.conversation.mapper.ChatConversationMapper;
import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.conversation.service.ChatConversationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

        return buildSingleConversationId(ownerUserId, peerUserId);
    }

    @Override
    public List<ChatConversationDO> listRecentSingleConversations(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        return chatConversationMapper.selectRecentByOwnerAndType(
                ownerUserId,
                ConversationType.SINGLE.getCode(),
                ConversationWindowTuning.RECENT_WINDOW_LIMIT
        );
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
                                                LocalDateTime lastMessageTime,
                                                Long lastServerMessageId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        if (lastServerMessageId == null || lastServerMessageId <= 0) {
            throw new IllegalArgumentException("lastServerMessageId is invalid");
        }
        String resolvedConversationId = buildSingleConversationId(senderId, receiverId);
        if (!resolvedConversationId.equals(conversationId)) {
            throw new IllegalStateException("sender conversation id does not match receiver conversation id");
        }

        Integer type = ConversationType.SINGLE.getCode();
        updateSenderConversationWithFallback(
                resolvedConversationId, senderId, receiverId, type, lastMessage, lastMessageTime, lastServerMessageId);
    }

    @Override
    public void updateReceiverConversationSummary(String conversationId,
                                                  Long senderId,
                                                  Long receiverId,
                                                  String lastMessage,
                                                  LocalDateTime lastMessageTime,
                                                  Long lastServerMessageId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        if (lastServerMessageId == null || lastServerMessageId <= 0) {
            throw new IllegalArgumentException("lastServerMessageId is invalid");
        }

        String resolvedConversationId = buildSingleConversationId(receiverId, senderId);
        if (!resolvedConversationId.equals(conversationId)) {
            throw new IllegalStateException("receiver conversation id does not match sender conversation id");
        }

        Integer type = ConversationType.SINGLE.getCode();
        updateReceiverConversationWithFallback(
                resolvedConversationId, receiverId, senderId, type, lastMessage, lastMessageTime, lastServerMessageId);
    }

    @Override
    public void projectSingleMessageToConversationSummaries(String conversationId,
                                                            Long senderId,
                                                            Long receiverId,
                                                            String lastMessage,
                                                            LocalDateTime lastMessageTime,
                                                            Long lastServerMessageId) {
        validateSingleProjection(conversationId, senderId, receiverId, lastServerMessageId);
        String resolvedConversationId = buildSingleConversationId(senderId, receiverId);
        Integer type = ConversationType.SINGLE.getCode();

        if (senderId <= receiverId) {
            updateSenderConversationWithFallback(
                    resolvedConversationId, senderId, receiverId, type, lastMessage, lastMessageTime, lastServerMessageId);
            updateReceiverConversationWithFallback(
                    resolvedConversationId, receiverId, senderId, type, lastMessage, lastMessageTime, lastServerMessageId);
            return;
        }

        updateReceiverConversationWithFallback(
                resolvedConversationId, receiverId, senderId, type, lastMessage, lastMessageTime, lastServerMessageId);
        updateSenderConversationWithFallback(
                resolvedConversationId, senderId, receiverId, type, lastMessage, lastMessageTime, lastServerMessageId);
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

    private int insertIgnoreConversation(String conversationId, Long ownerUserId, Long targetId, Integer type) {
        return chatConversationMapper.insertIgnoreConversation(
                conversationId,
                ownerUserId,
                targetId,
                type,
                0,
                0
        );
    }

    private void updateSenderConversationWithFallback(String conversationId,
                                                      Long ownerUserId,
                                                      Long targetId,
                                                      Integer type,
                                                      String lastMessage,
                                                      LocalDateTime lastMessageTime,
                                                      Long lastServerMessageId) {
        int updated = chatConversationMapper.updateSenderConversationSummary(
                conversationId, ownerUserId, targetId, type, lastMessage, lastMessageTime, lastServerMessageId);
        if (updated > 0) {
            return;
        }

        int inserted = insertIgnoreConversation(conversationId, ownerUserId, targetId, type);
        if (inserted > 0) {
            chatConversationMapper.updateSenderConversationSummary(
                    conversationId, ownerUserId, targetId, type, lastMessage, lastMessageTime, lastServerMessageId);
        }
    }

    private void updateReceiverConversationWithFallback(String conversationId,
                                                        Long ownerUserId,
                                                        Long targetId,
                                                        Integer type,
                                                        String lastMessage,
                                                        LocalDateTime lastMessageTime,
                                                        Long lastServerMessageId) {
        int updated = chatConversationMapper.updateReceiverConversationSummary(
                conversationId, ownerUserId, targetId, type, lastMessage, lastMessageTime, lastServerMessageId);
        if (updated > 0) {
            return;
        }

        int inserted = insertIgnoreConversation(conversationId, ownerUserId, targetId, type);
        if (inserted > 0) {
            chatConversationMapper.updateReceiverConversationSummary(
                    conversationId, ownerUserId, targetId, type, lastMessage, lastMessageTime, lastServerMessageId);
        }
    }

    private void validateSingleProjection(String conversationId,
                                          Long senderId,
                                          Long receiverId,
                                          Long lastServerMessageId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        if (lastServerMessageId == null || lastServerMessageId <= 0) {
            throw new IllegalArgumentException("lastServerMessageId is invalid");
        }
        String resolvedConversationId = buildSingleConversationId(senderId, receiverId);
        if (!resolvedConversationId.equals(conversationId)) {
            throw new IllegalStateException("single conversation id does not match participants");
        }
    }
}
