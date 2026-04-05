package com.bilibili.im.app.impl;

import com.bilibili.im.app.GroupApplicationService;
import com.bilibili.im.conversation.service.ChatGroupConversationService;
import com.bilibili.im.group.model.dto.CreateChatGroupDTO;
import com.bilibili.im.group.model.dto.InviteGroupMemberDTO;
import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;
import com.bilibili.im.group.model.enums.ChatGroupMemberRole;
import com.bilibili.im.group.model.enums.ChatGroupMemberStatus;
import com.bilibili.im.group.model.vo.ChatGroupMemberListVO;
import com.bilibili.im.group.model.vo.ChatGroupMemberVO;
import com.bilibili.im.group.model.vo.ChatGroupVO;
import com.bilibili.im.group.service.ChatGroupService;
import com.bilibili.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupApplicationServiceImpl implements GroupApplicationService {

    private final ChatGroupConversationService chatGroupConversationService;
    private final ChatGroupService chatGroupService;
    private final UserService userService;

    public GroupApplicationServiceImpl(ChatGroupConversationService chatGroupConversationService,
                                       ChatGroupService chatGroupService,
                                       UserService userService) {
        this.chatGroupConversationService = chatGroupConversationService;
        this.chatGroupService = chatGroupService;
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

        ChatGroupMemberDO inviterMembership = chatGroupService.getMembership(groupId, currentUserId);
        if (inviterMembership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(inviterMembership.getStatus())) {
            throw new IllegalArgumentException("group membership is invalid");
        }

        userService.validateUserExists(dto.getTargetUserId());
        chatGroupService.inviteMember(groupId, dto.getTargetUserId());
        chatGroupConversationService.initializeGroupConversation(dto.getTargetUserId(), groupId);
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

        ChatGroupMemberDO membership = chatGroupService.getMembership(groupId, currentUserId);
        if (membership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(membership.getStatus())) {
            throw new IllegalArgumentException("group membership is invalid");
        }

        if (Integer.valueOf(ChatGroupMemberRole.OWNER.getCode()).equals(membership.getRole())) {
            chatGroupService.dismissGroup(groupId, currentUserId);
            chatGroupConversationService.hideAllGroupConversations(groupId);
            return;
        }

        chatGroupService.leaveGroup(groupId, currentUserId);
        chatGroupConversationService.hideGroupConversation(currentUserId, groupId);
    }

    @Override
    public ChatGroupMemberListVO listGroupMembers(Long currentUserId, Long groupId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("currentUserId is invalid");
        }
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("groupId is invalid");
        }

        ChatGroupMemberDO membership = chatGroupService.getMembership(groupId, currentUserId);
        if (membership == null || !Integer.valueOf(ChatGroupMemberStatus.ACTIVE.getCode()).equals(membership.getStatus())) {
            throw new IllegalArgumentException("group membership is invalid");
        }

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
