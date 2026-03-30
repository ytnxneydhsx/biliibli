package com.bilibili.im.app.impl;

import com.bilibili.im.app.ConversationWindowApplicationService;
import com.bilibili.im.conversation.model.entity.ChatConversationDO;
import com.bilibili.im.conversation.service.ChatConversationService;
import com.bilibili.im.websocket.model.dto.ConversationWindowUpdateDTO;
import com.bilibili.im.websocket.service.ConversationWindowPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConversationWindowApplicationServiceImpl implements ConversationWindowApplicationService {

    private final ChatConversationService chatConversationService;
    private final ConversationWindowPushService conversationWindowPushService;

    public ConversationWindowApplicationServiceImpl(ChatConversationService chatConversationService,
                                                    ConversationWindowPushService conversationWindowPushService) {
        this.chatConversationService = chatConversationService;
        this.conversationWindowPushService = conversationWindowPushService;
    }

    @Override
    public void projectSingleMessageToConversationWindows(String conversationId,
                                                          Long senderId,
                                                          Long receiverId,
                                                          String lastMessage,
                                                          LocalDateTime lastMessageTime) {
        chatConversationService.projectSingleMessageToConversationSummaries(
                conversationId,
                senderId,
                receiverId,
                lastMessage,
                lastMessageTime
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
}
