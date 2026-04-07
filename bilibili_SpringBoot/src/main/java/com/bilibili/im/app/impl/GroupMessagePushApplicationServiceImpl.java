package com.bilibili.im.app.impl;

import com.bilibili.im.app.GroupMessagePushApplicationService;
import com.bilibili.im.group.mapper.ChatGroupMemberMapper;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberStatus;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.websocket.model.dto.MessagePushDTO;
import com.bilibili.im.websocket.service.MessagePushService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupMessagePushApplicationServiceImpl implements GroupMessagePushApplicationService {

    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final MessagePushService messagePushService;

    public GroupMessagePushApplicationServiceImpl(ChatGroupMemberMapper chatGroupMemberMapper,
                                                  MessagePushService messagePushService) {
        this.chatGroupMemberMapper = chatGroupMemberMapper;
        this.messagePushService = messagePushService;
    }

    @Override
    public void pushGroupMessage(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        if (event.getReceiverId() == null || event.getReceiverId() <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        List<ChatGroupMemberDO> activeMembers = chatGroupMemberMapper.selectByGroupIdAndStatus(
                event.getReceiverId(),
                ChatGroupMemberStatus.ACTIVE.getCode()
        );
        if (activeMembers == null || activeMembers.isEmpty()) {
            return;
        }

        MessagePushDTO message = toMessagePush(event);
        for (ChatGroupMemberDO member : activeMembers) {
            if (member == null || member.getUserId() == null || member.getUserId() <= 0) {
                continue;
            }
            messagePushService.pushMessageReceived(member.getUserId(), message);
        }
    }

    private MessagePushDTO toMessagePush(ImMessageDispatchEvent event) {
        MessagePushDTO messagePush = new MessagePushDTO();
        messagePush.setConversationId(event.getConversationId());
        messagePush.setSenderId(event.getSenderId());
        messagePush.setReceiverId(event.getReceiverId());
        messagePush.setClientMessageId(event.getClientMessageId());
        messagePush.setSenderLocation(event.getSenderLocation());
        messagePush.setMessageType(event.getMessageType());
        messagePush.setContent(event.getContent());
        messagePush.setSendTime(event.getSendTime());
        return messagePush;
    }
}
