package com.bilibili.im.group.service.impl;

import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.mapper.ChatGroupMemberMapper;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberRole;
import com.bilibili.im.group.model.enums.ChatGroupMemberStatus;
import com.bilibili.im.group.model.enums.ChatGroupMuteStatus;
import com.bilibili.im.group.model.enums.ChatGroupStatus;
import com.bilibili.im.group.permission.GroupPermissionService;
import com.bilibili.im.group.service.ChatGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatGroupServiceImpl implements ChatGroupService {

    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final GroupPermissionService groupPermissionService;

    public ChatGroupServiceImpl(ChatGroupMapper chatGroupMapper,
                                ChatGroupMemberMapper chatGroupMemberMapper,
                                GroupPermissionService groupPermissionService) {
        this.chatGroupMapper = chatGroupMapper;
        this.chatGroupMemberMapper = chatGroupMemberMapper;
        this.groupPermissionService = groupPermissionService;
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
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long groupId, Long targetUserId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }

        ChatGroupDO group = groupPermissionService.requireActiveGroup(groupId);
        ChatGroupMemberDO existedMember = chatGroupMemberMapper.selectByGroupIdAndUserId(groupId, targetUserId);
        if (existedMember != null && Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(existedMember.getStatus())) {
            throw new IllegalArgumentException("target user is already in group");
        }

        ChatGroupMemberDO member = new ChatGroupMemberDO();
        member.setGroupId(groupId);
        member.setUserId(targetUserId);
        member.setRole(ChatGroupMemberRole.MEMBER.getCode());
        member.setStatus(ChatGroupMemberStatus.ACTIVE.getCode());
        member.setIsMuted(ChatGroupMuteStatus.UNMUTED.getCode());
        member.setLastReadSeq(resolveLatestReadSeq(group));
        int rows = chatGroupMemberMapper.createOrReactivateMember(member);
        if (rows <= 0) {
            throw new RuntimeException("invite group member failed");
        }

        syncMemberCount(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long groupId, Long currentUserId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }

        ChatGroupMemberDO membership = groupPermissionService.requireActiveMembership(groupId, currentUserId);

        if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(membership.getRole())) {
            throw new IllegalArgumentException("owner should dismiss group instead of regular leave");
        }

        int rows = chatGroupMemberMapper.updateMemberStatus(
                groupId,
                currentUserId,
                ChatGroupMemberStatus.LEFT.getCode()
        );
        if (rows <= 0) {
            throw new RuntimeException("leave group failed");
        }

        syncMemberCount(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissGroup(Long groupId, Long ownerUserId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }

        ChatGroupDO group = groupPermissionService.requireActiveGroup(groupId);
        ChatGroupMemberDO membership = groupPermissionService.requireActiveMembership(groupId, ownerUserId);
        groupPermissionService.requireCanDismissGroup(group, membership, ownerUserId);

        int groupRows = chatGroupMapper.updateGroupStatusAndMemberCount(
                groupId,
                ChatGroupStatus.DISMISSED.getCode(),
                0
        );
        if (groupRows <= 0) {
            throw new RuntimeException("dismiss group failed");
        }

        int memberRows = chatGroupMemberMapper.batchUpdateMemberStatusByGroupId(
                groupId,
                ChatGroupMemberStatus.ACTIVE.getCode(),
                ChatGroupMemberStatus.REMOVED.getCode()
        );
        if (memberRows <= 0) {
            throw new RuntimeException("dismiss group members failed");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickGroupMember(Long groupId, Long operatorUserId, Long targetUserId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (operatorUserId == null || operatorUserId <= 0) {
            throw new IllegalArgumentException("operatorUserId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }
        groupPermissionService.requireActiveGroup(groupId);
        ChatGroupMemberDO operatorMembership = groupPermissionService.requireActiveMembership(groupId, operatorUserId);
        ChatGroupMemberDO targetMembership = groupPermissionService.requireActiveMembership(groupId, targetUserId);
        groupPermissionService.requireCanKickMember(operatorMembership, targetMembership, operatorUserId, targetUserId);

        int rows = chatGroupMemberMapper.updateMemberStatus(
                groupId,
                targetUserId,
                ChatGroupMemberStatus.REMOVED.getCode()
        );
        if (rows <= 0) {
            throw new RuntimeException("kick group member failed");
        }

        syncMemberCount(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateGroupName(Long groupId, Long operatorUserId, String groupName) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (operatorUserId == null || operatorUserId <= 0) {
            throw new IllegalArgumentException("operatorUserId is invalid");
        }
        String normalizedGroupName = groupName == null ? null : groupName.trim();
        if (normalizedGroupName == null || normalizedGroupName.isEmpty()) {
            throw new IllegalArgumentException("groupName is invalid");
        }

        groupPermissionService.requireActiveGroup(groupId);
        ChatGroupMemberDO operatorMembership = groupPermissionService.requireActiveMembership(groupId, operatorUserId);
        groupPermissionService.requireCanManageProfile(operatorMembership);

        int rows = chatGroupMapper.updateGroupName(groupId, normalizedGroupName);
        if (rows <= 0) {
            throw new RuntimeException("update group name failed");
        }
        return normalizedGroupName;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateGroupAvatar(Long groupId, Long operatorUserId, String groupAvatar) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (operatorUserId == null || operatorUserId <= 0) {
            throw new IllegalArgumentException("operatorUserId is invalid");
        }
        if (groupAvatar == null || groupAvatar.isBlank()) {
            throw new IllegalArgumentException("groupAvatar is invalid");
        }

        ChatGroupDO group = groupPermissionService.requireActiveGroup(groupId);
        ChatGroupMemberDO operatorMembership = groupPermissionService.requireActiveMembership(groupId, operatorUserId);
        groupPermissionService.requireCanManageProfile(operatorMembership);

        int rows = chatGroupMapper.updateGroupAvatar(groupId, groupAvatar);
        if (rows <= 0) {
            throw new RuntimeException("update group avatar failed");
        }
        return group.getGroupAvatar();
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

    private void syncMemberCount(Long groupId) {
        int activeMemberCount = chatGroupMemberMapper.countByGroupIdAndStatus(
                groupId,
                ChatGroupMemberStatus.ACTIVE.getCode()
        );
        int rows = chatGroupMapper.updateMemberCount(groupId, activeMemberCount);
        if (rows <= 0) {
            throw new RuntimeException("update group member count failed");
        }
    }

    private Long resolveLatestReadSeq(ChatGroupDO group) {
        if (group == null || group.getLastMessageSeq() == null || group.getLastMessageSeq() < 0) {
            return 0L;
        }
        return group.getLastMessageSeq();
    }
}
