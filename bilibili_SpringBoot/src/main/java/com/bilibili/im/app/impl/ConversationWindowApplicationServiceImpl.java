package com.bilibili.im.app.impl;

import com.bilibili.im.app.ConversationWindowApplicationService;
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
public class ConversationWindowApplicationServiceImpl implements ConversationWindowApplicationService {

    private final ChatConversationService chatConversationService;
    private final ConversationWindowCacheService conversationWindowCacheService;
    private final ConversationWindowPushService conversationWindowPushService;

    public ConversationWindowApplicationServiceImpl(ChatConversationService chatConversationService,
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
        ConversationWindowCacheValue redisWindow = conversationWindowCacheService.getConversationWindow(senderId, conversationId);
        ConversationWindowCacheValue persistedWindow = null;
        ConversationWindowCacheValue baselineWindow = redisWindow;
        if (baselineWindow == null) {
            persistedWindow = resolvePersistedConversationWindow(senderId, receiverId);
            baselineWindow = persistedWindow;
        }

        if (!shouldApplyEvent(baselineWindow, lastServerMessageId)) {
            if (redisWindow == null && isSameVersion(persistedWindow, lastServerMessageId)) {
                conversationWindowCacheService.cacheConversationWindowValue(senderId, persistedWindow);
            }
            return;
        }

        ConversationWindowCacheValue next = baselineWindow == null ? new ConversationWindowCacheValue() : baselineWindow;
        next.setConversationId(conversationId);
        next.setTargetId(receiverId);
        next.setLastMessage(lastMessage);
        next.setLastMessageTime(lastMessageTime);
        next.setLastServerMessageId(lastServerMessageId);
        next.setUnreadCount(resolveUnreadCount(next.getUnreadCount(), false));
        if (next.getIsMuted() == null) {
            next.setIsMuted(0);
        }

        conversationWindowCacheService.cacheConversationWindowValue(senderId, next);
        conversationWindowPushService.pushSingleConversationUpdated(senderId, toConversationWindowUpdate(next));
    }

    private void projectReceiverConversationToRedis(String conversationId,
                                                    Long senderId,
                                                    Long receiverId,
                                                    String lastMessage,
                                                    LocalDateTime lastMessageTime,
                                                    Long lastServerMessageId) {
        ConversationWindowCacheValue redisWindow = conversationWindowCacheService.getConversationWindow(receiverId, conversationId);
        ConversationWindowCacheValue persistedWindow = null;
        ConversationWindowCacheValue baselineWindow = redisWindow;
        if (baselineWindow == null) {
            persistedWindow = resolvePersistedConversationWindow(receiverId, senderId);
            baselineWindow = persistedWindow;
        }

        if (!shouldApplyEvent(baselineWindow, lastServerMessageId)) {
            if (redisWindow == null && isSameVersion(persistedWindow, lastServerMessageId)) {
                conversationWindowCacheService.cacheConversationWindowValue(receiverId, persistedWindow);
            }
            return;
        }

        ConversationWindowCacheValue next = baselineWindow == null ? new ConversationWindowCacheValue() : baselineWindow;
        next.setConversationId(conversationId);
        next.setTargetId(senderId);
        next.setLastMessage(lastMessage);
        next.setLastMessageTime(lastMessageTime);
        next.setLastServerMessageId(lastServerMessageId);
        next.setUnreadCount(resolveUnreadCount(next.getUnreadCount(), true));
        if (next.getIsMuted() == null) {
            next.setIsMuted(0);
        }

        conversationWindowCacheService.cacheConversationWindowValue(receiverId, next);
        conversationWindowPushService.pushSingleConversationUpdated(receiverId, toConversationWindowUpdate(next));
    }

    private ConversationWindowCacheValue resolvePersistedConversationWindow(Long ownerUserId, Long targetUserId) {
        ChatConversationDO persisted = chatConversationService.getSingleConversation(ownerUserId, targetUserId);
        if (persisted == null) {
            return null;
        }
        return toConversationWindowCacheValue(persisted);
    }

    private boolean shouldApplyEvent(ConversationWindowCacheValue baselineWindow, Long lastServerMessageId) {
        if (lastServerMessageId == null) {
            return true;
        }
        if (baselineWindow == null || baselineWindow.getLastServerMessageId() == null) {
            return true;
        }
        return baselineWindow.getLastServerMessageId() < lastServerMessageId;
    }

    private boolean isSameVersion(ConversationWindowCacheValue baselineWindow, Long lastServerMessageId) {
        return baselineWindow != null
                && baselineWindow.getLastServerMessageId() != null
                && baselineWindow.getLastServerMessageId().equals(lastServerMessageId);
    }

    private int resolveUnreadCount(Integer currentUnreadCount, boolean increment) {
        int unreadCount = currentUnreadCount == null ? 0 : Math.max(currentUnreadCount, 0);
        return increment ? unreadCount + 1 : unreadCount;
    }

    private ConversationWindowCacheValue toConversationWindowCacheValue(ChatConversationDO conversation) {
        ConversationWindowCacheValue value = new ConversationWindowCacheValue();
        value.setConversationId(conversation.getConversationId());
        value.setTargetId(conversation.getTargetId());
        value.setLastMessage(conversation.getLastMessage());
        value.setLastMessageTime(conversation.getLastMessageTime());
        value.setLastServerMessageId(conversation.getLastServerMessageId());
        value.setUnreadCount(conversation.getUnreadCount());
        value.setIsMuted(conversation.getIsMuted());
        return value;
    }

}
