package com.bilibili.im.group.permission.impl;

import com.bilibili.im.group.mapper.ChatGroupMapper;
import com.bilibili.im.group.mapper.ChatGroupMemberMapper;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberRole;
import com.bilibili.im.group.model.enums.ChatGroupMemberStatus;
import com.bilibili.im.group.model.enums.ChatGroupMuteStatus;
import com.bilibili.im.group.model.enums.ChatGroupStatus;
import com.bilibili.im.group.permission.GroupPermissionService;
import org.springframework.stereotype.Service;

@Service
public class GroupPermissionServiceImpl implements GroupPermissionService {

    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;

    public GroupPermissionServiceImpl(ChatGroupMapper chatGroupMapper,
                                      ChatGroupMemberMapper chatGroupMemberMapper) {
        this.chatGroupMapper = chatGroupMapper;
        this.chatGroupMemberMapper = chatGroupMemberMapper;
    }

    @Override
    public ChatGroupDO requireActiveGroup(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("group does not exist");
        }
        if (!Integer.valueOf(ChatGroupStatus.ACTIVE.getCode()).equals(group.getStatus())) {
            throw new IllegalArgumentException("group status is invalid");
        }
        return group;
    }

    @Override
    public ChatGroupMemberDO requireActiveMembership(Long groupId, Long userId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        ChatGroupMemberDO membership = chatGroupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (membership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(membership.getStatus())) {
            throw new IllegalArgumentException("group membership is invalid");
        }
        return membership;
    }

    @Override
    public void requireCanManageProfile(ChatGroupMemberDO membership) {
        if (membership == null) {
            throw new IllegalArgumentException("group membership is invalid");
        }
        Integer role = membership.getRole();
        if (!Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(role)
                && !Integer.valueOf(ChatGroupMemberRole.ADMIN.getCode()).equals(role)) {
            throw new IllegalArgumentException("no permission to manage group profile");
        }
    }

    @Override
    public void requireCanDismissGroup(ChatGroupDO group, ChatGroupMemberDO membership, Long operatorUserId) {
        if (group == null) {
            throw new IllegalArgumentException("group does not exist");
        }
        if (membership == null) {
            throw new IllegalArgumentException("group membership is invalid");
        }
        if (operatorUserId == null || operatorUserId <= 0) {
            throw new IllegalArgumentException("operatorUserId is invalid");
        }
        if (!operatorUserId.equals(group.getOwnerUserId())) {
            throw new IllegalArgumentException("only group owner can dismiss group");
        }
        if (!Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(membership.getRole())) {
            throw new IllegalArgumentException("only group owner can dismiss group");
        }
    }

    @Override
    public void requireCanKickMember(ChatGroupMemberDO operatorMembership,
                                     ChatGroupMemberDO targetMembership,
                                     Long operatorUserId,
                                     Long targetUserId) {
        if (operatorUserId == null || operatorUserId <= 0) {
            throw new IllegalArgumentException("operatorUserId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }
        if (operatorUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("cannot kick self");
        }
        if (operatorMembership == null) {
            throw new IllegalArgumentException("operator membership is invalid");
        }
        if (targetMembership == null) {
            throw new IllegalArgumentException("target membership is invalid");
        }

        Integer operatorRole = operatorMembership.getRole();
        Integer targetRole = targetMembership.getRole();

        if (Integer.valueOf(ChatGroupMemberRole.MEMBER.getCode()).equals(operatorRole)) {
            throw new IllegalArgumentException("member cannot kick others");
        }

        if (Integer.valueOf(ChatGroupMemberRole.ADMIN.getCode()).equals(operatorRole)) {
            if (!Integer.valueOf(ChatGroupMemberRole.MEMBER.getCode()).equals(targetRole)) {
                throw new IllegalArgumentException("admin can only kick normal members");
            }
            return;
        }

        if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(operatorRole)) {
            if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(targetRole)) {
                throw new IllegalArgumentException("owner cannot kick owner");
            }
            return;
        }

        throw new IllegalArgumentException("operator role is invalid");
    }

    @Override
    public void requireCanChangeMemberRole(ChatGroupMemberDO operatorMembership,
                                           ChatGroupMemberDO targetMembership,
                                           Integer targetRole) {
        if (targetRole == null) {
            throw new IllegalArgumentException("targetRole is invalid");
        }
        if (operatorMembership == null) {
            throw new IllegalArgumentException("operator membership is invalid");
        }
        if (targetMembership == null) {
            throw new IllegalArgumentException("target membership is invalid");
        }
        if (operatorMembership.getUserId() != null
                && operatorMembership.getUserId().equals(targetMembership.getUserId())) {
            throw new IllegalArgumentException("cannot update self role");
        }
        if (!Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(operatorMembership.getRole())) {
            throw new IllegalArgumentException("only group owner can update member role");
        }
        if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(targetMembership.getRole())) {
            throw new IllegalArgumentException("cannot update owner role");
        }
        if (!Integer.valueOf(ChatGroupMemberRole.ADMIN.getCode()).equals(targetRole)
                && !Integer.valueOf(ChatGroupMemberRole.MEMBER.getCode()).equals(targetRole)) {
            throw new IllegalArgumentException("target role is invalid");
        }
    }

    @Override
    public void requireCanUpdateGroupMuteStatus(ChatGroupMemberDO operatorMembership,
                                                Integer isMuted) {
        if (isMuted == null) {
            throw new IllegalArgumentException("isMuted is invalid");
        }
        if (!Integer.valueOf(ChatGroupMuteStatus.UNMUTED.getCode()).equals(isMuted)
                && !Integer.valueOf(ChatGroupMuteStatus.MUTED.getCode()).equals(isMuted)) {
            throw new IllegalArgumentException("isMuted is invalid");
        }
        requireCanManageProfile(operatorMembership);
    }

    @Override
    public void requireCanUpdateMemberMuteStatus(ChatGroupMemberDO operatorMembership,
                                                 ChatGroupMemberDO targetMembership,
                                                 Integer isMuted) {
        if (isMuted == null) {
            throw new IllegalArgumentException("isMuted is invalid");
        }
        if (!Integer.valueOf(ChatGroupMuteStatus.UNMUTED.getCode()).equals(isMuted)
                && !Integer.valueOf(ChatGroupMuteStatus.MUTED.getCode()).equals(isMuted)) {
            throw new IllegalArgumentException("isMuted is invalid");
        }
        if (operatorMembership == null) {
            throw new IllegalArgumentException("operator membership is invalid");
        }
        if (targetMembership == null) {
            throw new IllegalArgumentException("target membership is invalid");
        }
        if (operatorMembership.getUserId() != null
                && operatorMembership.getUserId().equals(targetMembership.getUserId())) {
            throw new IllegalArgumentException("cannot update self mute status");
        }

        Integer operatorRole = operatorMembership.getRole();
        Integer targetRole = targetMembership.getRole();

        if (Integer.valueOf(ChatGroupMemberRole.MEMBER.getCode()).equals(operatorRole)) {
            throw new IllegalArgumentException("member cannot mute others");
        }

        if (Integer.valueOf(ChatGroupMemberRole.ADMIN.getCode()).equals(operatorRole)) {
            if (!Integer.valueOf(ChatGroupMemberRole.MEMBER.getCode()).equals(targetRole)) {
                throw new IllegalArgumentException("admin can only mute normal members");
            }
            return;
        }

        if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(operatorRole)) {
            if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(targetRole)) {
                throw new IllegalArgumentException("owner cannot mute owner");
            }
            return;
        }

        throw new IllegalArgumentException("operator role is invalid");
    }
}
