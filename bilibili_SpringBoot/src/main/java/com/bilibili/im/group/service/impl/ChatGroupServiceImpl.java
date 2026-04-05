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
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long groupId, Long targetUserId) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }

        ChatGroupDO group = getGroup(groupId);
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

        ChatGroupMemberDO membership = getMembership(groupId, currentUserId);
        if (membership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(membership.getStatus())) {
            throw new IllegalArgumentException("group membership is invalid");
        }

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

        ChatGroupDO group = getGroup(groupId);
        if (!Integer.valueOf(ChatGroupStatus.ACTIVE.getCode()).equals(group.getStatus())) {
            throw new IllegalArgumentException("group status is invalid");
        }
        if (!ownerUserId.equals(group.getOwnerUserId())) {
            throw new IllegalArgumentException("only group owner can dismiss group");
        }

        ChatGroupMemberDO membership = getMembership(groupId, ownerUserId);
        if (membership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(membership.getStatus())) {
            throw new IllegalArgumentException("group membership is invalid");
        }
        if (!Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(membership.getRole())) {
            throw new IllegalArgumentException("only group owner can dismiss group");
        }

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
        if (operatorUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("cannot kick self");
        }

        ChatGroupDO group = getGroup(groupId);
        if (!Integer.valueOf(ChatGroupStatus.ACTIVE.getCode()).equals(group.getStatus())) {
            throw new IllegalArgumentException("group status is invalid");
        }

        ChatGroupMemberDO operatorMembership = getMembership(groupId, operatorUserId);
        if (operatorMembership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(operatorMembership.getStatus())) {
            throw new IllegalArgumentException("operator membership is invalid");
        }

        ChatGroupMemberDO targetMembership = getMembership(groupId, targetUserId);
        if (targetMembership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(targetMembership.getStatus())) {
            throw new IllegalArgumentException("target membership is invalid");
        }

        validateKickPermission(operatorMembership, targetMembership);

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

    private void validateKickPermission(ChatGroupMemberDO operatorMembership,
                                        ChatGroupMemberDO targetMembership) {
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
}
