package com.bilibili.im.conversation.service.impl;

import com.bilibili.im.conversation.mapper.ChatGroupConversationMapper;
import com.bilibili.im.conversation.model.entity.ChatGroupConversationDO;
import com.bilibili.im.conversation.model.enums.ChatGroupConversationStatus;
import com.bilibili.im.conversation.model.vo.GroupConversationWindowVO;
import com.bilibili.im.conversation.service.ChatGroupConversationService;
import com.bilibili.im.group.model.enums.ChatGroupMuteStatus;
import com.bilibili.im.group.model.enums.ChatGroupStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatGroupConversationServiceImpl implements ChatGroupConversationService {

    private final ChatGroupConversationMapper chatGroupConversationMapper;

    public ChatGroupConversationServiceImpl(ChatGroupConversationMapper chatGroupConversationMapper) {
        this.chatGroupConversationMapper = chatGroupConversationMapper;
    }

    @Override
    public String resolveGroupConversationId(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        return "g_" + groupId;
    }

    @Override
    public void initializeGroupConversation(Long ownerUserId, Long groupId, Long lastReadSeq) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (lastReadSeq == null || lastReadSeq < 0) {
            throw new IllegalArgumentException("lastReadSeq is invalid");
        }

        ChatGroupConversationDO conversation = new ChatGroupConversationDO();
        conversation.setConversationId(resolveGroupConversationId(groupId));
        conversation.setOwnerUserId(ownerUserId);
        conversation.setGroupId(groupId);
        conversation.setStatus(ChatGroupConversationStatus.ACTIVE.getCode());
        conversation.setIsMuted(ChatGroupMuteStatus.UNMUTED.getCode());
        conversation.setLastReadSeq(lastReadSeq);
        int rows = chatGroupConversationMapper.createOrShowConversation(conversation);
        if (rows <= 0) {
            throw new RuntimeException("initialize group conversation failed");
        }
    }

    @Override
    public void hideGroupConversation(Long ownerUserId, Long groupId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        int rows = chatGroupConversationMapper.updateConversationStatus(
                ownerUserId,
                groupId,
                ChatGroupConversationStatus.HIDDEN_AFTER_EXIT.getCode()
        );
        if (rows <= 0) {
            throw new RuntimeException("hide group conversation failed");
        }
    }

    @Override
    public void hideAllGroupConversations(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        int rows = chatGroupConversationMapper.batchUpdateConversationStatusByGroupId(
                groupId,
                ChatGroupConversationStatus.HIDDEN_AFTER_EXIT.getCode()
        );
        if (rows <= 0) {
            throw new RuntimeException("hide all group conversations failed");
        }
    }

    @Override
    public List<GroupConversationWindowVO> listVisibleGroupConversations(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        return chatGroupConversationMapper.selectVisibleGroupWindowsByOwnerUserId(
                ownerUserId,
                ChatGroupConversationStatus.ACTIVE.getCode(),
                ChatGroupStatus.ACTIVE.getCode()
        );
    }
}
