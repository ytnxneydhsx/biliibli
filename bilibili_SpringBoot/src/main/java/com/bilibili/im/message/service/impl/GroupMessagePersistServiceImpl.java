package com.bilibili.im.message.service.impl;

import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.mapper.ChatGroupMessageMapper;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMessageDO;
import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.service.ChatMessageService;
import com.bilibili.im.message.service.GroupMessagePersistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class GroupMessagePersistServiceImpl implements GroupMessagePersistService {

    private final ChatMessageService chatMessageService;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMessageMapper chatGroupMessageMapper;

    public GroupMessagePersistServiceImpl(ChatMessageService chatMessageService,
                                          ChatGroupMapper chatGroupMapper,
                                          ChatGroupMessageMapper chatGroupMessageMapper) {
        this.chatMessageService = chatMessageService;
        this.chatGroupMapper = chatGroupMapper;
        this.chatGroupMessageMapper = chatGroupMessageMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistGroupMessage(Long groupId, PersistMessageCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (!Integer.valueOf(ConversationType.GROUP.getCode()).equals(command.getConversationType())) {
            throw new IllegalArgumentException("conversationType is invalid");
        }

        ChatGroupDO group = chatGroupMapper.selectByIdForUpdate(groupId);
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("group does not exist");
        }

        ChatMessageDO message = chatMessageService.persistMessage(command);
        long nextSeq = resolveNextSeq(group);

        ChatGroupMessageDO groupMessage = new ChatGroupMessageDO();
        groupMessage.setGroupId(groupId);
        groupMessage.setMessageId(message.getId());
        groupMessage.setGroupMessageSeq(nextSeq);
        int rows = chatGroupMessageMapper.insert(groupMessage);
        if (rows != 1 || groupMessage.getId() == null) {
            throw new RuntimeException("insert chat group message failed");
        }

        int summaryRows = chatGroupMapper.updateLatestMessage(
                groupId,
                buildGroupSummary(command.getContent()),
                command.getSendTime(),
                command.getServerMessageId(),
                nextSeq
        );
        if (summaryRows != 1) {
            throw new RuntimeException("update group latest message failed");
        }
    }

    private long resolveNextSeq(ChatGroupDO group) {
        Long current = group.getLastMessageSeq();
        return (current == null ? 0L : current) + 1L;
    }

    private String buildGroupSummary(MessageContentDTO content) {
        if (content == null) {
            return "[消息]";
        }
        String text = content.getText() == null ? null : content.getText().trim();
        if (text != null && !text.isEmpty()) {
            return text;
        }
        List<String> imageUrls = content.getImageUrls() == null ? Collections.emptyList() : content.getImageUrls();
        if (!imageUrls.isEmpty()) {
            return "[图片]";
        }
        return "[消息]";
    }
}
