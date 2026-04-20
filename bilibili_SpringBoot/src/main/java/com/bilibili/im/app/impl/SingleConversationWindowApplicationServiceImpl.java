package com.bilibili.im.app.impl;

import com.bilibili.im.app.SingleConversationWindowApplicationService;
import com.bilibili.im.conversation.cache.ConversationWindowCacheService;
import com.bilibili.im.conversation.cache.model.ConversationWindowCacheValue;
import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import com.bilibili.im.conversation.model.vo.ConversationWindowListVO;
import com.bilibili.im.conversation.model.vo.ConversationWindowVO;
import com.bilibili.im.conversation.service.ChatConversationService;
import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SingleConversationWindowApplicationServiceImpl implements SingleConversationWindowApplicationService {

    private final ChatConversationService chatConversationService;
    private final ConversationWindowCacheService conversationWindowCacheService;
    private final ConversationWindowPushService conversationWindowPushService;

    public SingleConversationWindowApplicationServiceImpl(ChatConversationService chatConversationService,
                                                          ConversationWindowCacheService conversationWindowCacheService,
                                                          ConversationWindowPushService conversationWindowPushService) {
        this.chatConversationService = chatConversationService;
        this.conversationWindowCacheService = conversationWindowCacheService;
        this.conversationWindowPushService = conversationWindowPushService;
    }

    @Override
    public ConversationWindowListVO listRecentConversations(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }

        List<ConversationWindowVO> cached = conversationWindowCacheService.listRecentConversations(ownerUserId);
        if (cached != null) {
            ConversationWindowListVO result = new ConversationWindowListVO();
            result.setOwnerUserId(ownerUserId);
            result.setSize(cached.size());
            result.setRecords(cached);
            return result;
        }

        List<ConversationWindowVO> records = chatConversationService.listRecentSingleConversations(ownerUserId)
                .stream()
                .map(this::toConversationWindowVO)
                .toList();
        conversationWindowCacheService.replaceRecentConversations(ownerUserId, records);

        ConversationWindowListVO result = new ConversationWindowListVO();
        result.setOwnerUserId(ownerUserId);
        result.setSize(records.size());
        result.setRecords(records);
        return result;
    }

    @Override
    public void projectSingleMessageToConversationWindows(String conversationId,
                                                          Long senderId,
                                                          Long receiverId,
                                                          String lastMessage,
                                                          LocalDateTime lastMessageTime,
                                                          Long lastServerMessageId) {
        chatConversationService.projectSingleMessageToConversationSummaries(
                conversationId,
                senderId,
                receiverId,
                lastMessage,
                lastMessageTime,
                lastServerMessageId
        );
    }

    @Override
    public void projectSingleMessageToRedisConversationWindows(String conversationId,
                                                               Long senderId,
                                                               Long receiverId,
                                                               String lastMessage,
                                                               LocalDateTime lastMessageTime,
                                                               Long lastServerMessageId) {
        projectSenderConversationToRedis(
                conversationId,
                senderId,
                receiverId,
                lastMessage,
                lastMessageTime,
                lastServerMessageId
        );
        projectReceiverConversationToRedis(
                conversationId,
                senderId,
                receiverId,
                lastMessage,
                lastMessageTime,
                lastServerMessageId
        );
    }

    @Override
    public void pushUpdatedSingleConversationWindows(Long senderId, Long receiverId) {
        pushSingleConversationWindow(senderId, receiverId);
        pushSingleConversationWindow(receiverId, senderId);
    }

    @Override
    public void clearSingleConversationUnread(Long ownerUserId, Long peerUserId) {
        chatConversationService.clearSingleConversationUnread(ownerUserId, peerUserId);
        pushSingleConversationWindow(ownerUserId, peerUserId);
    }

    private void pushSingleConversationWindow(Long ownerUserId, Long targetUserId) {
        ChatConversationDO conversation = chatConversationService.getSingleConversation(ownerUserId, targetUserId);
        if (conversation == null) {
            return;
        }
        conversationWindowCacheService.cacheConversationWindow(ownerUserId, toConversationWindowVO(conversation));
        conversationWindowPushService.pushSingleConversationUpdated(
                ownerUserId,
                toConversationWindowUpdate(conversation)
        );
    }

    private ConversationWindowUpdateDTO toConversationWindowUpdate(ChatConversationDO conversation) {
        ConversationWindowUpdateDTO update = new ConversationWindowUpdateDTO();
        update.setConversationId(conversation.getConversationId());
        update.setTargetUserId(conversation.getTargetId());
        update.setLastMessage(conversation.getLastMessage());
        update.setLastMessageTime(conversation.getLastMessageTime());
        update.setUnreadCount(conversation.getUnreadCount());
        return update;
    }

    private ConversationWindowUpdateDTO toConversationWindowUpdate(ConversationWindowCacheValue conversation) {
        ConversationWindowUpdateDTO update = new ConversationWindowUpdateDTO();
        update.setConversationId(conversation.getConversationId());
        update.setTargetUserId(conversation.getTargetId());
        update.setLastMessage(conversation.getLastMessage());
        update.setLastMessageTime(conversation.getLastMessageTime());
        update.setUnreadCount(conversation.getUnreadCount());
        return update;
    }

    private ConversationWindowVO toConversationWindowVO(ChatConversationDO conversation) {
        ConversationWindowVO vo = new ConversationWindowVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setTargetId(conversation.getTargetId());
        vo.setLastMessage(conversation.getLastMessage());
        vo.setLastMessageTime(conversation.getLastMessageTime());
        vo.setLastServerMessageId(conversation.getLastServerMessageId());
        vo.setUnreadCount(conversation.getUnreadCount());
        vo.setIsMuted(conversation.getIsMuted());
        return vo;
    }

    private void projectSenderConversationToRedis(String conversationId,
                                                  Long senderId,
                                                  Long receiverId,
                                                  String lastMessage,
                                                  LocalDateTime lastMessageTime,
                                                  Long lastServerMessageId) {
        if (!conversationWindowCacheService.isInitialized(senderId)) {
            return;
        }
        ensureRedisConversationBaseline(senderId, receiverId, conversationId);
        ConversationWindowCacheValue updated = conversationWindowCacheService.projectConversationWindowEvent(
                senderId,
                conversationId,
                receiverId,
                lastMessage,
                lastMessageTime,
                lastServerMessageId,
                false
        );
        if (updated != null) {
            conversationWindowPushService.pushSingleConversationUpdated(senderId, toConversationWindowUpdate(updated));
        }
    }

    private void projectReceiverConversationToRedis(String conversationId,
                                                    Long senderId,
                                                    Long receiverId,
                                                    String lastMessage,
                                                    LocalDateTime lastMessageTime,
                                                    Long lastServerMessageId) {
        if (!conversationWindowCacheService.isInitialized(receiverId)) {
            return;
        }
        ensureRedisConversationBaseline(receiverId, senderId, conversationId);
        ConversationWindowCacheValue updated = conversationWindowCacheService.projectConversationWindowEvent(
                receiverId,
                conversationId,
                senderId,
                lastMessage,
                lastMessageTime,
                lastServerMessageId,
                true
        );
        if (updated != null) {
            conversationWindowPushService.pushSingleConversationUpdated(receiverId, toConversationWindowUpdate(updated));
        }
    }

    private void ensureRedisConversationBaseline(Long ownerUserId, Long targetUserId, String conversationId) {
        if (conversationWindowCacheService.getConversationWindow(ownerUserId, conversationId) != null) {
            return;
        }
        ConversationWindowCacheValue persisted = resolvePersistedConversationWindow(ownerUserId, targetUserId);
        if (persisted != null) {
            conversationWindowCacheService.cacheConversationWindowBaselineIfAbsent(ownerUserId, persisted);
        }
    }

    private ConversationWindowCacheValue resolvePersistedConversationWindow(Long ownerUserId, Long targetUserId) {
        ChatConversationDO persisted = chatConversationService.getSingleConversation(ownerUserId, targetUserId);
        if (persisted == null) {
            return null;
        }
        return toConversationWindowCacheValue(persisted);
    }

    private ConversationWindowCacheValue toConversationWindowCacheValue(ChatConversationDO conversation) {
        ConversationWindowCacheValue value = new ConversationWindowCacheValue();
        value.setConversationId(conversation.getConversationId());
        value.setTargetId(conversation.getTargetId());
        value.setLastMessage(conversation.getLastMessage());
        value.setLastMessageTime(conversation.getLastMessageTime());
        value.setLastServerMessageId(conversation.getLastServerMessageId());
        if (conversation.getLastServerMessageId() != null) {
            String serverMessageId = String.valueOf(conversation.getLastServerMessageId());
            value.setUnreadBaselineServerMessageIdText(serverMessageId);
        }
        value.setUnreadCount(conversation.getUnreadCount());
        value.setIsMuted(conversation.getIsMuted());
        return value;
    }
}
