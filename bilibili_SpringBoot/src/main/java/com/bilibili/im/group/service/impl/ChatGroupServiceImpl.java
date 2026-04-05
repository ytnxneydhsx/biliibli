package com.bilibili.im.group.service.impl;

import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.mapper.ChatGroupMemberMapper;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberRole;
import com.bilibili.im.group.model.enums.ChatGroupMemberStatus;
import com.bilibili.im.group.model.enums.ChatGroupMuteStatus;
import com.bilibili.im.group.model.enums.ChatGroupStatus;
import com.bilibili.im.group.service.ChatGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatGroupServiceImpl implements ChatGroupService {

    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;

    public ChatGroupServiceImpl(ChatGroupMapper chatGroupMapper,
                                ChatGroupMemberMapper chatGroupMemberMapper) {
        this.chatGroupMapper = chatGroupMapper;
        this.chatGroupMemberMapper = chatGroupMemberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupDO createGroup(Long ownerUserId, String groupName) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        String normalizedGroupName = groupName == null ? null : groupName.trim();
        if (normalizedGroupName == null || normalizedGroupName.isEmpty()) {
            throw new IllegalArgumentException("groupName is invalid");
        }

        ChatGroupDO group = new ChatGroupDO();
        group.setGroupName(normalizedGroupName);
        group.setOwnerUserId(ownerUserId);
        group.setStatus(ChatGroupStatus.ACTIVE.getCode());
        group.setMemberCount(1);
        group.setIsAllMuted(ChatGroupMuteStatus.UNMUTED.getCode());
        group.setLastMessageSeq(0L);
        int groupRows = chatGroupMapper.insert(group);
        if (groupRows <= 0 || group.getId() == null || group.getId() <= 0) {
            throw new RuntimeException("create chat group failed");
        }

        ChatGroupMemberDO ownerMember = new ChatGroupMemberDO();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(ownerUserId);
        ownerMember.setRole(ChatGroupMemberRole.OWNER.getCode());
        ownerMember.setStatus(ChatGroupMemberStatus.ACTIVE.getCode());
        ownerMember.setIsMuted(ChatGroupMuteStatus.UNMUTED.getCode());
        ownerMember.setLastReadSeq(resolveLatestReadSeq(group));
        int memberRows = chatGroupMemberMapper.createOrReactivateMember(ownerMember);
        if (memberRows <= 0) {
            throw new RuntimeException("create chat group owner membership failed");
        }

        return group;
    }

    @Override
    public ChatGroupDO getGroup(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("group does not exist");
        }
        return group;
    }

    @Override
    public ChatGroupMemberDO getMembership(Long groupId, Long userId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        return chatGroupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public List<ChatGroupMemberDO> listActiveMembers(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        return chatGroupMemberMapper.selectByGroupIdAndStatus(groupId, ChatGroupMemberStatus.ACTIVE.getCode());
    }

    private Long resolveLatestReadSeq(ChatGroupDO group) {
        if (group == null || group.getLastMessageSeq() == null || group.getLastMessageSeq() < 0) {
            return 0L;
        }
        return group.getLastMessageSeq();
    }
}
