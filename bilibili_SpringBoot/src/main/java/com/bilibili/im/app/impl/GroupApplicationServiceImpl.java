package com.bilibili.im.app.impl;

import com.bilibili.im.app.GroupApplicationService;
import com.bilibili.im.conversation.service.ChatGroupConversationService;
import com.bilibili.im.group.model.dto.CreateChatGroupDTO;
import com.bilibili.im.group.model.dto.InviteGroupMemberDTO;
import com.bilibili.im.group.model.dto.UpdateChatGroupNameDTO;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberRole;
import com.bilibili.im.group.model.vo.ChatGroupMemberListVO;
import com.bilibili.im.group.model.vo.ChatGroupMemberVO;
import com.bilibili.im.group.model.vo.ChatGroupVO;
import com.bilibili.im.group.permission.GroupPermissionService;
import com.bilibili.im.group.service.ChatGroupService;
import com.bilibili.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupApplicationServiceImpl implements GroupApplicationService {

    private final ChatGroupConversationService chatGroupConversationService;
    private final ChatGroupService chatGroupService;
    private final GroupPermissionService groupPermissionService;
    private final UserService userService;

    public GroupApplicationServiceImpl(ChatGroupConversationService chatGroupConversationService,
                                       ChatGroupService chatGroupService,
                                       GroupPermissionService groupPermissionService,
                                       UserService userService) {
        this.chatGroupConversationService = chatGroupConversationService;
        this.chatGroupService = chatGroupService;
        this.groupPermissionService = groupPermissionService;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupVO createGroup(Long currentUserId, CreateChatGroupDTO dto) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("dto is invalid");
        }

        ChatGroupDO group = chatGroupService.createGroup(currentUserId, dto.getGroupName());
        chatGroupConversationService.initializeGroupConversation(currentUserId, group.getId());
        return toGroupVO(group);
    }

    @Override
    public ChatGroupVO getGroup(Long groupId) {
        return toGroupVO(chatGroupService.getGroup(groupId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteGroupMember(Long currentUserId, Long groupId, InviteGroupMemberDTO dto) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (dto == null || dto.getTargetUserId() == null || dto.getTargetUserId() <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }

        groupPermissionService.requireActiveMembership(groupId, currentUserId);

        userService.validateUserExists(dto.getTargetUserId());
        chatGroupService.inviteMember(groupId, dto.getTargetUserId());
        chatGroupConversationService.initializeGroupConversation(dto.getTargetUserId(), groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateGroupName(Long currentUserId, Long groupId, UpdateChatGroupNameDTO dto) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("dto is invalid");
        }
        return chatGroupService.updateGroupName(groupId, currentUserId, dto.getGroupName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long currentUserId, Long groupId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        ChatGroupMemberDO membership = groupPermissionService.requireActiveMembership(groupId, currentUserId);

        if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(membership.getRole())) {
            chatGroupService.dismissGroup(groupId, currentUserId);
            chatGroupConversationService.hideAllGroupConversations(groupId);
            return;
        }

        chatGroupService.leaveGroup(groupId, currentUserId);
        chatGroupConversationService.hideGroupConversation(currentUserId, groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickGroupMember(Long currentUserId, Long groupId, Long targetUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }

        chatGroupService.kickGroupMember(groupId, currentUserId, targetUserId);
        chatGroupConversationService.hideGroupConversation(targetUserId, groupId);
    }

    @Override
    public ChatGroupMemberListVO listGroupMembers(Long currentUserId, Long groupId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        groupPermissionService.requireActiveMembership(groupId, currentUserId);

        List<ChatGroupMemberVO> records = chatGroupService.listActiveMembers(groupId)
                .stream()
                .map(this::toGroupMemberVO)
                .toList();

        ChatGroupMemberListVO result = new ChatGroupMemberListVO();
        result.setGroupId(groupId);
        result.setSize(records.size());
        result.setRecords(records);
        return result;
    }

    private ChatGroupVO toGroupVO(ChatGroupDO group) {
        ChatGroupVO vo = new ChatGroupVO();
        vo.setGroupId(group.getId());
        vo.setConversationId(chatGroupConversationService.resolveGroupConversationId(group.getId()));
        vo.setGroupName(group.getGroupName());
        vo.setOwnerUserId(group.getOwnerUserId());
        vo.setGroupAvatar(group.getGroupAvatar());
        vo.setStatus(group.getStatus());
        vo.setMemberCount(group.getMemberCount());
        vo.setIsAllMuted(group.getIsAllMuted());
        vo.setLastMessage(group.getLastMessage());
        vo.setLastMessageTime(group.getLastMessageTime());
        vo.setLastServerMessageId(group.getLastServerMessageId());
        vo.setLastMessageSeq(group.getLastMessageSeq());
        return vo;
    }

    private ChatGroupMemberVO toGroupMemberVO(ChatGroupMemberDO member) {
        ChatGroupMemberVO vo = new ChatGroupMemberVO();
        vo.setUserId(member.getUserId());
        vo.setRole(member.getRole());
        vo.setStatus(member.getStatus());
        vo.setIsMuted(member.getIsMuted());
        vo.setLastReadSeq(member.getLastReadSeq());
        return vo;
    }
}
